package com.template.flows.attachment

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.AttachmentContract
import com.template.states.AttachmentState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class AttachmentRegisterFlow (private val attachId: String, private val counterParty: String) : FlowLogic<SignedTransaction>(){

    @Suspendable
    override fun call(): SignedTransaction
    {
        val updating = update()

        val signedTransaction = verifyAndSign(transaction = updating)
        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
        val sessions = initiateFlow(counterRef)
        val transactionSignedByAllParties = collectSignature(transaction = signedTransaction, sessions = listOf(sessions))
        return verifyRegistration(transaction = transactionSignedByAllParties, sessions = listOf(sessions))

    }

    private fun outState(): AttachmentState
    {
        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")

        return AttachmentState(
                SecureHash.sha256(attachId),
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

@InitiatedBy(AttachmentRegisterFlow::class)
class AttachmentFlowResponder (val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession)
        {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an attachment transaction." using (output is AttachmentState)
            }
        }
        val signedTransaction = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }
}