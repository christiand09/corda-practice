package com.template.flows.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.RegisterContract
import com.template.states.RegisterState
import net.corda.core.contracts.*
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
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
class VerifyFlow (private val counterParty: String,
                  private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>()
{
    override val progressTracker: ProgressTracker = tracker()

    companion object
    {
        object CREATING : ProgressTracker.Step("Creating verification!")
        object SIGNING : ProgressTracker.Step("Signing verification!")
        object VERIFYING : ProgressTracker.Step("Verifying verification!")
        object NOTARIZING : ProgressTracker.Step("Notarize verification")
        object FINALISING : ProgressTracker.Step("Finalize verification!")
        {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(CREATING, SIGNING, VERIFYING, NOTARIZING, FINALISING)
    }

    @Suspendable
    override fun call(): SignedTransaction
    {
        progressTracker.currentStep = CREATING
        val verification = verify()

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(transaction = verification)
        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
            ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
        val sessions = initiateFlow(counterRef)
        val transactionSignedByAllParties = collectSignature(transaction = signedTransaction, sessions = listOf(sessions))

        progressTracker.currentStep = NOTARIZING
        progressTracker.currentStep = FINALISING
        return verifyRegistration(transaction = transactionSignedByAllParties, sessions = listOf(sessions))
    }

    private fun inputStateRef(): StateAndRef<RegisterState>{
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<RegisterState>(criteria = criteria).states.single()
    }

    private fun outState(): RegisterState
    {
        val input = inputStateRef().state.data
        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")

        return RegisterState(
                input.name,
                ourIdentity,
                counterRef,
                approved = true,
                linearId = linearId
        )
    }

    private fun verify(): TransactionBuilder
    {
        val notary = inputStateRef().state.notary
        val verifyCommand =
                Command(RegisterContract.Commands.Verify(),
                        outState().participants.map { it.owningKey })

        return TransactionBuilder(notary = notary).withItems(inputStateRef(), StateAndContract(state = outState(), contract = RegisterContract.REGISTER_ID), verifyCommand)
    }

    private fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction
    {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    @Suspendable
    private fun collectSignature(
            transaction: SignedTransaction,
            sessions: List<FlowSession>
    ): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, sessions))

    @Suspendable
    private fun verifyRegistration(
            transaction: SignedTransaction,
            sessions: List<FlowSession>
    ): SignedTransaction = subFlow(FinalityFlow(transaction, sessions))
}

@InitiatedBy(VerifyFlow::class)
class VerifyFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession)
        {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a verify transaction" using (output is RegisterState)
            }
        }

        val signedTransaction = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }
}
