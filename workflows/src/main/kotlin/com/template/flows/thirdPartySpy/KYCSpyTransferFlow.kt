package com.template.flows.thirdPartySpy

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.KYCSpyContract
import com.template.states.KYCSpyState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class KYCSpyTransferFlow (
        private val borrower: String,
        private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {


    @Suspendable
    override fun call(): SignedTransaction {
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val spySession = initiateFlow(spy)
        spySession.send(false)

        val counterRef = serviceHub.identityService.partiesFromName(borrower, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $borrower.")
        val sessions = initiateFlow(counterRef)
        sessions.send(true)

        val signedTransaction = verifyAndSign(transaction(spy))
        val transactionSignedByParties = collectSignature(signedTransaction, listOf(sessions))

        return recordRegistration(transactionSignedByParties,  listOf(sessions,spySession))
    }

    private fun inputStateRef(): StateAndRef<KYCSpyState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<KYCSpyState>(criteria = criteria).states.single()
    }

    private fun outputState(): KYCSpyState
    {
        val input = inputStateRef().state.data
        val counterRef = serviceHub.identityService.partiesFromName(borrower, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $borrower.")
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val requestAmount = input.requestedAmount



        return KYCSpyState(


                moneyLend = input.moneyLend.plus(requestAmount),
                moneyBalance = input.moneyBalance.plus(requestAmount),
                requestedAmount = input.requestedAmount.minus(requestAmount),
                lender = ourIdentity,
                borrower = counterRef,
                spy = spy,
                status = "Request Approved!",
                linearId = input.linearId


        )
    }

    private fun transaction(spy: Party): TransactionBuilder
    {
        val spiedOnMessage = outputState().copy(participants = outputState().participants + spy)
        val notary = inputStateRef().state.notary
        val counterRef = serviceHub.identityService.partiesFromName(borrower, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $borrower.")
        val issueCommand =
                Command(KYCSpyContract.Commands.Transfer(), listOf(ourIdentity.owningKey,counterRef.owningKey))

        return TransactionBuilder(notary = notary)
                .addInputState(inputStateRef())
                .addOutputState(spiedOnMessage, KYCSpyContract.ID)
                .addCommand(issueCommand)
    }

    private fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    @Suspendable
    private fun collectSignature(
            transaction: SignedTransaction,
            sessions: List<FlowSession>
    ): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, sessions))

    @Suspendable
    private fun recordRegistration(
            transaction: SignedTransaction,
            sessions: List<FlowSession>
    ): SignedTransaction = subFlow(FinalityFlow(transaction, sessions))
}

@InitiatedBy(KYCSpyTransferFlow::class)
class SpyTransfer(private val session: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // receive the flag
        val needsToSignTransaction = session.receive<Boolean>().unwrap { it }
        // only sign if instructed to do so
        if (needsToSignTransaction) {
            subFlow(object : SignTransactionFlow(session) {
                override fun checkTransaction(stx: SignedTransaction) { }
            })
        }
        // always save the transaction
        return subFlow(ReceiveFinalityFlow(otherSideSession = session))
    }
}