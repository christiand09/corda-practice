package com.template.flows.UserFlow

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.AttachmentContract
import com.template.states.AttachmentState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class UploadAttachmentFlow (private val hash: String, private val counterparty:String):FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction {

        val attachmenttransaction : TransactionBuilder = upload(attachmentstate())
        val signedTransaction: SignedTransaction = verifyandsign(attachmenttransaction)
        val counterRef = serviceHub.identityService.partiesFromName(counterparty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterparty.")
        val session = initiateFlow(counterRef)
        val signedbybothparties: SignedTransaction = signedTransaction
        val transactionsignedbyall = collectsignatures(signedTransaction, listOf(session))
        return recordAttachment(transaction = transactionsignedbyall,session = listOf(session))

    }
    private fun attachmentstate(): AttachmentState
    {
        val counterRef = serviceHub.identityService.partiesFromName(counterparty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterparty.")
        val stringHash = SecureHash.parse(hash)
        return AttachmentState(stringHash,ourIdentity,counterRef)
    }

    private fun upload(state: AttachmentState): TransactionBuilder
    {
        val notary :Party = serviceHub.networkMapCache.notaryIdentities.first()
        val counterRef = serviceHub.identityService.partiesFromName(counterparty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterparty.")
        val command = Command(AttachmentContract.Commands.Attachment(),listOf(counterRef.owningKey,ourIdentity.owningKey))
        val builder =  TransactionBuilder(notary = notary)
                .addOutputState(state = state,contract = AttachmentContract.ATTACHMENT_ID)
                .addCommand(command)
        return builder
    }
    private fun verifyandsign(transaction:TransactionBuilder): SignedTransaction
    {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }
    @Suspendable
    private fun collectsignatures(transaction: SignedTransaction,session: List<FlowSession>):SignedTransaction
            = subFlow(CollectSignaturesFlow(transaction,session))

    @Suspendable
    private fun recordAttachment(transaction: SignedTransaction,session: List<FlowSession>):SignedTransaction
    = subFlow(FinalityFlow(transaction,session))

    @InitiatedBy(UploadAttachmentFlow::class)
    class IOUIssueFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an IOU transaction" using (output is AttachmentState)
                }
            }

            val txWeJustSignedId = subFlow(signedTransactionFlow)

            return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
        }
    }
}
