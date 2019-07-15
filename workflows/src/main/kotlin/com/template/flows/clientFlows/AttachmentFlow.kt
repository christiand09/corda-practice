package com.template.flows.clientFlows

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
class AttachmentFlow(private val counterParty: String,
                     private val attachId: String) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call() : SignedTransaction {
        val update = update()
        val signedTransaction = verifyAndSign(update)
        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
        val session = initiateFlow(counterRef)
        val transactionSignedByAllParties = collectSignature(signedTransaction, listOf(session))

        return verifyRegistration(transactionSignedByAllParties, listOf(session))
    }

    private fun outputState() : AttachmentState {
        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
        return AttachmentState(SecureHash.parse(attachId), ourIdentity, counterRef)
    }

    private fun update() : TransactionBuilder {
        val notary: Party = serviceHub.networkMapCache.notaryIdentities.first()
        val attachmentCommand = Command(AttachmentContract.Attach,outputState().participants.map { it.owningKey })
        val builder = TransactionBuilder(notary)
                .addOutputState(outputState(), AttachmentContract.ATTACHMENT_ID)
                .addCommand(attachmentCommand)
                .addAttachment(SecureHash.parse(attachId))
        return builder
    }

    private fun verifyAndSign(transaction: TransactionBuilder) : SignedTransaction {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    @Suspendable
    private fun verifyRegistration(transaction: SignedTransaction, session: List<FlowSession>) :
            SignedTransaction = subFlow(FinalityFlow(transaction, session))

    @Suspendable
    private fun collectSignature(transaction: SignedTransaction, session: List<FlowSession>) :
            SignedTransaction = subFlow(CollectSignaturesFlow(transaction, session))
}

@InitiatedBy(AttachmentFlow::class)
class AttachmentFlowResponder (val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call() : SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be Attachment." using (output is AttachmentState)
            }
        }
        val signedTransaction = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }
}