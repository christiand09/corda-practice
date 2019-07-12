package com.template.flows.UserContentsFlows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.ATTACHMENT_PROGRAM_ID
import com.template.contracts.AttachmentsContract
import com.template.flows.*
import com.template.states.AttachmentsState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker



@InitiatingFlow

@StartableByRPC

class AttachmentsFlow(private val receiver: String,
                      private val attachId: String) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker(
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            NOTARIZE_TRANSACTION,
            FINALISING_TRANSACTION)

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = GENERATING_TRANSACTION
        val attachment = Attachments()

        progressTracker.currentStep = VERIFYING_TRANSACTION
        progressTracker.currentStep = SIGNING_TRANSACTION
        val signedTransaction = verifyAndSign(transaction = attachment)

        val counterRef = serviceHub.identityService.partiesFromName(receiver, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $receiver.")

        val sessions = initiateFlow(counterRef)
        val allsignature = collectSignature(signedTransaction, listOf(sessions))
        progressTracker.currentStep = FINALISING_TRANSACTION



        return recordTransaction(transaction = allsignature, sessions = listOf(sessions))

    }
    private fun outputState(): AttachmentsState {
        val counterRef = serviceHub.identityService.partiesFromName(receiver, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $receiver.")

        val hash = SecureHash.sha256(attachId)
        return AttachmentsState(sender = ourIdentity,receiver = counterRef,attachId = hash)

    }

    private fun Attachments(): TransactionBuilder{
        val counterRef = serviceHub.identityService.partiesFromName(receiver, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $receiver.")
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val cmd = Command(AttachmentsContract.Attach, listOf(counterRef.owningKey,ourIdentity.owningKey))
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(outputState(), ATTACHMENT_PROGRAM_ID)
                .addCommand(cmd)
               // .addAttachment(SecureHash.parse(attachId))
        return txBuilder
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
    private fun recordTransaction(transaction: SignedTransaction, sessions: List<FlowSession>): SignedTransaction =
            subFlow(FinalityFlow(transaction, sessions))










}
//    object SIGNING : ProgressTracker.Step("Signing transaction")
//    override val progressTracker: ProgressTracker = ProgressTracker(SIGNING)
//
//    @Suspendable
//    override fun call(): SignedTransaction {
//        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//        // Create a trivial transaction with an output that describes the attachment, and the attachment itself
//        val ptx = TransactionBuilder(notary)
//                .addOutputState(AttachmentsContract.State(attachId), ATTACHMENT_PROGRAM_ID)
//                .addCommand(AttachmentsContract.Command, ourIdentity.owningKey)
//                .addAttachment(attachId)
//
//        progressTracker.currentStep = SIGNING
//
//        val stx = serviceHub.signInitialTransaction(ptx)
//        // Send the transaction to the other recipient
//        return subFlow(FinalityFlow(stx, initiateFlow(receiver)))
//
//    }
//
//}










@InitiatedBy(AttachmentsFlow::class)
class AttachmentFlowResponder (val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession)
        {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an attachment transaction." using (output is AttachmentsState)
            }
        }
        val signedTransaction = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))

    }

}

