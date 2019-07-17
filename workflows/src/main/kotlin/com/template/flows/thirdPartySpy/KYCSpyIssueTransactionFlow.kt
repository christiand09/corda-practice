package com.template.flows.thirdPartySpy

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.KYCSpyContract
import com.template.states.KYCSpyState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class KYCSpyIssueTransactionFlow (private val amount: Long,
                               private val lender: String,
                               private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {


    @Suspendable
    override fun call(): SignedTransaction {
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val spySession = initiateFlow(spy)
        spySession.send(false)

        val counterRef = serviceHub.identityService.partiesFromName(lender, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $lender.")
        val sessions = initiateFlow(counterRef)
        sessions.send(true)


        val signedTransaction = verifyAndSign(transaction())
        val transactionSignedByParties = collectSignature(signedTransaction, listOf(sessions))
        return recordRegistration(transactionSignedByParties, listOf(sessions,spySession))
    }

    private fun inputStateRef(): StateAndRef<KYCSpyState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<KYCSpyState>(criteria).states.single()
    }

    private fun outputState(): KYCSpyState
    {
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val input = inputStateRef().state.data
        val counterRef = serviceHub.identityService.partiesFromName(lender, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $lender.")

        return KYCSpyState(
                moneyLend = input.moneyLend,
                moneyBalance = input.moneyBalance,
                requestedAmount = input.requestedAmount + amount,
                lender = counterRef,
                borrower = ourIdentity,
                spy = spy,
                status = "Requesting to lend $amount",
                linearId = input.linearId,
                participants = listOf(ourIdentity, counterRef, spy)


        )
    }

    private fun transaction(): TransactionBuilder
    {
        val notary = inputStateRef().state.notary
        val counterRef = serviceHub.identityService.partiesFromName(lender, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $lender.")
        val issueCommand =
                Command(KYCSpyContract.Commands.Issue(), listOf(ourIdentity.owningKey,counterRef.owningKey))

        val builder = TransactionBuilder(notary = notary)
        builder.addInputState(inputStateRef())
        builder.addOutputState(outputState(), KYCSpyContract.ID)
        builder.addCommand(issueCommand)
        return builder


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

@InitiatedBy(KYCSpyIssueTransactionFlow::class)
class KYCSpyIssueTransactionFlowResponder(private val session: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
//         receive the flag
        val needsToSignTransaction = session.receive<Boolean>().unwrap { it }
        // only sign if instructed to do so
        if (needsToSignTransaction) {
            subFlow(object : SignTransactionFlow(session) {
                override fun checkTransaction(stx: SignedTransaction) { }
            })
        }
//         always save the transaction
        return subFlow(ReceiveFinalityFlow(otherSideSession = session))
    }
}