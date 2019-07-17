package com.template.flows.tokenAmount

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.KYCContract
import com.template.states.KYCState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class KYCTransferFlow (
                               private val borrower: String,
                               private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {


    @Suspendable
    override fun call(): SignedTransaction {

        val moneyLending = moneyLending()
        val signedTransaction = verifyAndSign(moneyLending)
        val counterRef = serviceHub.identityService.partiesFromName(borrower, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $borrower.")
        val sessions = initiateFlow(counterRef)
        val transactionSignedByParties = collectSignature(transaction = signedTransaction, sessions = listOf(sessions))

        if (ourIdentity != inputStateRef().state.data.lender) {
            throw IllegalArgumentException("KYC settlement flow must be initiated by the lender.")
        }

        return recordRegistration(transaction = transactionSignedByParties, sessions = listOf(sessions))
    }

    private fun inputStateRef(): StateAndRef<KYCState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<KYCState>(criteria = criteria).states.single()
    }

    private fun outState(): KYCState
    {
        val input = inputStateRef().state.data
        val counterRef = serviceHub.identityService.partiesFromName(borrower, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $borrower.")

        val requestAmount = input.requestedAmount



        return KYCState(


                moneyLend = input.moneyLend.plus(requestAmount),
                moneyBalance = input.moneyBalance.plus(requestAmount),
                requestedAmount = input.requestedAmount.minus(requestAmount),
                lender = ourIdentity,
                borrower = counterRef,
                status = "Request Approved!",
                linearId = input.linearId


        )
    }

    private fun moneyLending(): TransactionBuilder
    {
        val notary = inputStateRef().state.notary
        val issueCommand =
                Command(KYCContract.Commands.Transfer(), outState().participants.map { it.owningKey })

        return TransactionBuilder( notary)
                .addInputState(inputStateRef())
                .addOutputState(outState(), KYCContract.ID)
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

@InitiatedBy(KYCTransferFlow::class)
class TransactionFlow(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction
    {
        val signTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an issue transaction" using (output is KYCState)
            }
        }
        val signedTransaction = subFlow(signTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }
}
