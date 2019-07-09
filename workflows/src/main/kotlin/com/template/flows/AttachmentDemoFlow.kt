package com.template.flows

import co.paralleluniverse.fibers.Suspendable

import com.template.contracts.AttachmentContract
import com.template.contracts.AttachmentContract.Companion.ATTACHMENT_PROGRAM_ID
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import com.template.states.AttachmentState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat


@InitiatingFlow
@StartableByRPC
class AttachmentDemoFlow(private val counterPartyy: String,
                         private val attachId: SecureHash.SHA256) : FlowLogic<SignedTransaction>() {

    //    object SIGNING : ProgressTracker.Step("Signing transaction")
//    override val progressTracker: ProgressTracker = ProgressTracker(SIGNING)
    @Suspendable
    override fun call(): SignedTransaction {
        // Create a trivial transaction with an output that describes the attachment, and the attachment itself
        val counterRef = serviceHub.identityService.partiesFromName(counterPartyy, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterPartyy.")
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val ptx = TransactionBuilder(notary = notary)
                .addOutputState(outputState(),ATTACHMENT_PROGRAM_ID)
                .addCommand(AttachmentContract.Attach, outputState().participants.map { it.owningKey })
                .addAttachment(attachId)
        val stx = serviceHub.signInitialTransaction(ptx)
        // Send the transaction to the other recipient
        return subFlow(FinalityFlow(stx, initiateFlow(counterRef)))
    }
    private fun outputState(): AttachmentState
    {
        val counterRef = serviceHub.identityService.partiesFromName(counterPartyy, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterPartyy.")
        return AttachmentState(
                attachId,
                ourIdentity,
                counterRef
        )
    }
    @InitiatedBy(AttachmentDemoFlow::class)
    class AttachmentDemoFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an attachment transaction." using (output is AttachmentState)
                }
            }
            val signedTransaction = subFlow(signedTransactionFlow)
            return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
        }
    }
}


//@InitiatedBy(AttachmentDemoFlow::class)
//class StoreAttachmentFlow(private val otherSide: FlowSession) : FlowLogic<Unit>() {
//    @Suspendable
//    override fun call() {
//        // As a non-participant to the transaction we need to record all states
//        subFlow(ReceiveFinalityFlow(otherSide, statesToRecord = StatesToRecord.ALL_VISIBLE))
//    }
//}
//@StartableByRPC
//@StartableByService
//class NoProgressTrackerShellDemo : FlowLogic<String>() {
//    @Suspendable
//    override fun call(): String {
//        return "You Called me!"
//    }
//}