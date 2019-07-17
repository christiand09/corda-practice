package com.template.flows.clientSampleRegister

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.ClientContract
import com.template.contracts.ClientContract.Companion.ID
import com.template.states.Calls
import com.template.states.ClientState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class ClientUpdateRegisterFlow(
        private var calls: Calls,
        private val counterparty: String,
        private val linearId: UniqueIdentifier): FlowLogic<SignedTransaction>(){

    /* Declare Transaction steps*/

    companion object{
        object BUILDING_TRANSACTION : ProgressTracker.Step("Building Transaction")
        object SIGN_TRANSACTION : ProgressTracker.Step("Signing Transaction")
        object VERIFY_TRANSACTION : ProgressTracker.Step("Verifying Transaction")
        object NOTARIZE_TRANSACTION : ProgressTracker.Step("Notarizing Transaction")
        object RECORD_TRANSACTION : ProgressTracker.Step("Recording Transaction")
    }

    fun tracker() = ProgressTracker(
            BUILDING_TRANSACTION,
            SIGN_TRANSACTION,
            VERIFY_TRANSACTION,
            NOTARIZE_TRANSACTION,
            RECORD_TRANSACTION
    )


    override val progressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = BUILDING_TRANSACTION
        progressTracker.currentStep = VERIFY_TRANSACTION
        progressTracker.currentStep = SIGN_TRANSACTION
        progressTracker.currentStep = NOTARIZE_TRANSACTION
        progressTracker.currentStep = RECORD_TRANSACTION
        val unUpdated= userUpdate()
        val signedTransaction = verifyAndSign(unUpdated)
        val counterRef = serviceHub.identityService.partiesFromName(counterparty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterparty.")
        val sessions= initiateFlow(counterRef)
        val transactionSignedByAllParties: SignedTransaction=collectSignature(signedTransaction, listOf(sessions))
        sessions.send(calls)
        return recordTransaction(transactionSignedByAllParties, listOf(sessions))
    }
    private fun inputStateRef(): StateAndRef<ClientState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))

        return serviceHub.vaultService.queryBy<ClientState>(criteria).states.first()
    }
    private fun outputState(): ClientState{
        val input = inputStateRef().state.data

        if(calls.name == "")
            calls.name = input.calls.name
        if(calls.age == "")
            calls.age = input.calls.age
        if(calls.address == "")
            calls.address = input.calls.address
        if(calls.birthDate == "")
            calls.birthDate = input.calls.birthDate
        if(calls.status == "")
            calls.status = input.calls.status
        if(calls.religion == "")
            calls.religion = input.calls.religion

        val counterRef = serviceHub.identityService.partiesFromName(counterparty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterparty.")

        return ClientState(calls,ourIdentity,counterRef,true,input.linearId)

    }
    private fun userUpdate(): TransactionBuilder{
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val cmd = Command(ClientContract.Commands.Update(), outputState().participants.map { it.owningKey })
        return TransactionBuilder (notary)
                .addInputState(inputStateRef())
                .addOutputState(outputState(), ID)
                .addCommand(cmd)
    }
    private fun verifyAndSign(transaction : TransactionBuilder): SignedTransaction{
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

@InitiatedBy(ClientUpdateRegisterFlow::class)
class ClientUpdateRegisterFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction" using (output is ClientState)
            }
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)
        val payload = flowSession.receive(Calls::class.java).unwrap { it }
        subFlow(ClientRegisterFlow(payload))
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}