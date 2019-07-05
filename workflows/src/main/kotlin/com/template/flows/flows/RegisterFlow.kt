package com.template.flows.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.RegisterContract
import com.template.states.Name
import com.template.states.RegisterState
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
import net.corda.core.utilities.ProgressTracker.Step

@InitiatingFlow
@StartableByRPC
class RegisterFlow (private val name: Name) : FlowLogic<SignedTransaction>()
{
    override val progressTracker: ProgressTracker = tracker()

    companion object
    {
        object CREATING : Step("Creating registration!")
        object SIGNING : Step("Signing registration!")
        object VERIFYING : Step("Verifying registration!")
        object NOTARIZING : Step("Notarizing registration!")
        object FINALISING : Step("Finalize registration!")
        {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(CREATING, SIGNING, VERIFYING, NOTARIZING, FINALISING)
    }

    @Suspendable
    override fun call(): SignedTransaction
    {
        progressTracker.currentStep = CREATING
        val registration = register(outState())

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(transaction = registration)
//        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
//                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
//        val sessions = initiateFlow(counterRef)
        val sessions = emptyList<FlowSession>() // empty because the owner's signature is just needed
        val transactionSignedByParties = collectSignature(transaction = signedTransaction, sessions = sessions)

        progressTracker.currentStep = NOTARIZING
        progressTracker.currentStep = FINALISING
        return recordRegistration(transaction = transactionSignedByParties, sessions = sessions)
    }

    private fun outState(): RegisterState
    {
//        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
//                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
        return RegisterState(
                name,
                ourIdentity,
//                counterRef,
                ourIdentity,
                false
        )
    }

    private fun register(state: RegisterState): TransactionBuilder
    {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val registerCommand =
                Command(RegisterContract.Commands.Register(), ourIdentity.owningKey)

        return TransactionBuilder(notary = notary).withItems(StateAndContract(state = state, contract = RegisterContract.REGISTER_ID), registerCommand)
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

@InitiatedBy(RegisterFlow::class)
class RegisterFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val signTransactionFlow = object : SignTransactionFlow(flowSession)
        {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a registration flow" using (output is RegisterState)
            }
        }
        val signedTransaction = subFlow(signTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }


}

