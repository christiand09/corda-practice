package com.template.flows.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.AttachmentContract
import com.template.flows.cashfunctions.*
import com.template.states.AttachmentState
import net.corda.core.contracts.*
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class AttachmentFlow (private val counterParty: String,
                      private val attachId: String,
                      private val attachId2: String,
                      private val attachId3: String) : CashFunctions()
{
    override val progressTracker = ProgressTracker(
            CREATING, VERIFYING, SIGNING, NOTARIZING, FINALIZING
    )

    @Suspendable
    override fun call(): SignedTransaction
    {
        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")

        progressTracker.currentStep = CREATING
        val updating = update()

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(transaction = updating)
        val sessions = emptyList<FlowSession>()
        val transactionSignedByAllParties = collectSignature(transaction = signedTransaction, sessions = sessions)

        progressTracker.currentStep = NOTARIZING
        progressTracker.currentStep = FINALIZING
        return recordTransactionWithOtherParty(transaction = transactionSignedByAllParties, sessions = sessions)
    }

    private fun outState(): AttachmentState
    {
        val hash = SecureHash.sha256(attachId)
        val hash2 = SecureHash.sha256(attachId2)
        val hash3 = SecureHash.sha256(attachId3)

        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")

        return AttachmentState(
                hash,
//                hash2,
//                hash3,
                ourIdentity,
                counterRef
        )
    }

    private fun update(): TransactionBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first()).apply {
        val attachmentCommand =
                Command(AttachmentContract.Attach,
                        outState().participants.map { it.owningKey })
        addOutputState(outState(), AttachmentContract.ATTACHMENT_PROGRAM_ID)
        addAttachment(outState().hash)
//        addAttachment(outState().hash2)
//        addAttachment(outState().hash3)
        addCommand(attachmentCommand)
    }
}