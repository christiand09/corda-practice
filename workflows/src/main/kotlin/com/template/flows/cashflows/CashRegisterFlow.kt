package com.template.flows.cashflows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.CashContract
import com.template.states.CashState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class CashRegisterFlow : FlowLogic<SignedTransaction>()
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
    override fun call(): SignedTransaction
    {
        progressTracker.currentStep = CREATING
        val registration = registerCash()

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(transaction = registration)
        val sessions = emptyList<FlowSession>() // empty because the owner's signature is just needed
        val transactionSignedByParties = collectSignature(transaction = signedTransaction, sessions = sessions)

        progressTracker.currentStep = NOTARIZING
        progressTracker.currentStep = FINALISING
        return recordRegistration(transaction = transactionSignedByParties, sessions = sessions)
    }

    private fun outState(): CashState
    {
        return CashState(
                amount = 0,
                request = false,
                status = "pending",
                borrower = ourIdentity,
                lender = ourIdentity,
                wallet = 0
        )
    }

    private fun registerCash(): TransactionBuilder
    {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val cashRegisterCommand = Command(
                CashContract.Commands.Register(),
                ourIdentity.owningKey)
        return TransactionBuilder(notary = notary).withItems(StateAndContract(state = outState(), contract = CashContract.CASH_ID), cashRegisterCommand)
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

@InitiatedBy(CashRegisterFlow::class)
class RegisterFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val signTransactionFlow = object : SignTransactionFlow(flowSession)
        {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a registration flow" using (output is CashState)
            }
        }
        val signedTransaction = subFlow(signTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }
}