package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.AttachmentContract
import com.template.states.AttachmentState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class AttachmentFlow (private val counterParty: String, private val attachId: SecureHash.SHA256) : FlowLogic<SignedTransaction>()
{
    override val progressTracker: ProgressTracker = tracker()

    companion object
    {
        object CREATING : ProgressTracker.Step("Creating update!")
        object SIGNING : ProgressTracker.Step("Signing update!")
        object VERIFYING : ProgressTracker.Step("Verifying update!")
        object NOTARIZING : ProgressTracker.Step("Notarize update")
        object FINALISING : ProgressTracker.Step("Finalize update!") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(CREATING, SIGNING, VERIFYING, NOTARIZING, FINALISING)
    }

    @Suspendable
    override fun call(): SignedTransaction
    {
        progressTracker.currentStep = CREATING
        val updating = update()

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(transaction = updating)
        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
        val sessions = initiateFlow(counterRef)
        val transactionSignedByAllParties = collectSignature(transaction = signedTransaction, sessions = listOf(sessions))

        progressTracker.currentStep = NOTARIZING
        progressTracker.currentStep = FINALISING
        return verifyRegistration(transaction = transactionSignedByAllParties, sessions = listOf(sessions))
    }

    private fun outState(): AttachmentState
    {
        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")

        return AttachmentState(
                attachId,
                ourIdentity,
                counterRef
        )
    }

    private fun update(): TransactionBuilder
    {

        val contract = AttachmentContract.ATTACHMENT_PROGRAM_ID
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val attachmentCommand =
                Command(AttachmentContract.Attach,
                        outState().participants.map { it.owningKey })

        return TransactionBuilder(notary = notary).withItems(StateAndContract(outState(), contract), attachmentCommand)
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

