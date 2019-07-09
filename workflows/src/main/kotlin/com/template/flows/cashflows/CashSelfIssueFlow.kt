package com.template.flows.cashflows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.CashContract
import com.template.states.CashState
import net.corda.core.contracts.*
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class CashSelfIssueFlow (val issueAmount: Long, private val linearId: UniqueIdentifier): FlowLogic<SignedTransaction>()
{
    override val progressTracker: ProgressTracker = tracker()

    companion object
    {
        object CREATING : ProgressTracker.Step("Creating registration!")
        object SIGNING : ProgressTracker.Step("Signing registration!")
        object VERIFYING : ProgressTracker.Step("Verifying registration!")
        object NOTARIZING : ProgressTracker.Step("Notarizing registration!")
        object FINALISING : ProgressTracker.Step("Finalize registration!")
        {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(CREATING, SIGNING, VERIFYING, NOTARIZING, FINALISING)
    }

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = CREATING
        val selfIssuance = selfIssue()

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(transaction = selfIssuance)
        val sessions = emptyList<FlowSession>() // empty because the owner's signature is just needed
        val transactionSignedByParties = collectSignature(transaction = signedTransaction, sessions = sessions)

        progressTracker.currentStep = NOTARIZING
        progressTracker.currentStep = FINALISING
        return recordRegistration(transaction = transactionSignedByParties, sessions = sessions)
    }

    private fun inputStateRef(): StateAndRef<CashState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<CashState>(criteria = criteria).states.single()
    }

    private fun outState(): CashState
    {
        val input = inputStateRef().state.data

        return CashState(
                amount = input.amount,
                request = input.request,
                status = input.status,
                borrower = ourIdentity,
                lender = ourIdentity,
                wallet = input.wallet.plus(issueAmount),
                linearId = linearId
        )
    }

    private fun selfIssue(): TransactionBuilder
    {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val selfIssueCommand =
                Command(CashContract.Commands.SelfIssue(), ourIdentity.owningKey)
        return TransactionBuilder(notary = notary)
                .addInputState(inputStateRef())
                .addOutputState(outState(), CashContract.CASH_ID)
                .addCommand(selfIssueCommand)
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

@InitiatedBy(CashSelfIssueFlow::class)
class CashSelfIssueFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val signTransactionFlow = object : SignTransactionFlow(flowSession)
        {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a self issue cash flow" using (output is CashState)
            }
        }
        val signedTransaction = subFlow(signTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }
}