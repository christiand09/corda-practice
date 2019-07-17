package com.template.flows.tokenAmount

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.KYCContract
import com.template.states.KYCState
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.seconds
import java.time.Instant

@InitiatingFlow
@StartableByRPC
class KYCIssueTransactionFlow (private val amount: Long,
                     private val lender: String,
                     private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {


    @Suspendable
    override fun call(): SignedTransaction {

        val moneyLending = moneyLending()
        val signedTransaction = verifyAndSign(moneyLending)
        val counterRef = serviceHub.identityService.partiesFromName(lender, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $lender.")
        val sessions = initiateFlow(counterRef)
        val transactionSignedByParties = collectSignature(signedTransaction, listOf(sessions))
        if (ourIdentity != inputStateRef().state.data.borrower) {
            throw IllegalArgumentException("KYC settlement flow must be initiated by the borrower.")
        }
        return recordRegistration(transactionSignedByParties, listOf(sessions))
    }

    private fun inputStateRef(): StateAndRef<KYCState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))

        return serviceHub.vaultService.queryBy<KYCState>(criteria).states.single()

    }

    private fun outputState(): KYCState
    {


        val input = inputStateRef().state.data
        val counterRef = serviceHub.identityService.partiesFromName(lender, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $lender.")

        return KYCState(

                moneyLend = input.moneyLend,
                moneyBalance = input.moneyBalance,
                requestedAmount = input.requestedAmount.plus(amount),
                lender = counterRef,
                borrower = ourIdentity,
                status = "Requesting to lend $amount",
                linearId = input.linearId

        )
    }

    private fun moneyLending(): TransactionBuilder
    {


        val ourTimeWindow: TimeWindow = TimeWindow.between(Instant.MIN, Instant.MAX)
//        val ourAfter: TimeWindow = TimeWindow.fromOnly(Instant.MIN)
//        val ourBefore: TimeWindow = TimeWindow.untilOnly(Instant.MAX)




//        val ourTimeWindow2: TimeWindow = TimeWindow.withTolerance(serviceHub.clock.instant(), 30.seconds)
//        val ourTimeWindow3: TimeWindow = TimeWindow.fromStartAndDuration(serviceHub.clock.instant(), 30.seconds)
//
//        transactionBuilder.setTimeWindow(ourTimeWindow)
//        txBuilder.setTimeWindow(serviceHub.clock.instant(), 45.seconds)




        val notary = inputStateRef().state.notary
        val issueCommand =
                Command(KYCContract.Commands.Issue(), outputState().participants.map { it.owningKey })

        return TransactionBuilder(notary)
                .addInputState(inputStateRef())
                .addOutputState(outputState(), KYCContract.ID)
                .addCommand(issueCommand)
                .setTimeWindow(ourTimeWindow)
                .setTimeWindow(serviceHub.clock.instant(), 45.seconds)
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

@InitiatedBy(KYCIssueTransactionFlow::class)
class IssueTransactionFlow(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {
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

