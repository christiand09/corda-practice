package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.ClientContract
import com.template.states.Calls
import com.template.states.ClientState
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker


@InitiatingFlow
@StartableByRPC
class ClientUpdateRegisterFlow(
        private var calls: Calls,
        private val counterparty: Party,
        private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {


    companion object {
        object BUILDING_TRANSACTION : ProgressTracker.Step("Building Transaction")
        object SIGN_TRANSACTION : ProgressTracker.Step("Signing Transaction")
        object VERIFY_TRANSACTION : ProgressTracker.Step("Verifying Transaction")
        object NOTARIZE_TRANSACTION : ProgressTracker.Step("Notarizing Transaction")
        object RECORD_TRANSACTION : ProgressTracker.Step("Recording Transaction")
    }

    fun tracker() = ProgressTracker(
            ClientRegisterFlow.Companion.BUILDING_TRANSACTION,
            ClientRegisterFlow.Companion.SIGN_TRANSACTION,
            ClientRegisterFlow.Companion.VERIFY_TRANSACTION,
            ClientRegisterFlow.Companion.NOTARIZE_TRANSACTION,
            ClientRegisterFlow.Companion.RECORD_TRANSACTION
    )

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction
    {

        val updating = update()


        val signedTransaction = verifyAndSign(transaction = updating)
        val sessions = initiateFlow(counterparty)
        sessions.send(calls)
        val transactionSignedByAllParties = collectSignature(transaction = signedTransaction, sessions = listOf(sessions))



        return verifyRegistration(transaction = transactionSignedByAllParties, sessions = listOf(sessions))
    }

    private fun inputStateRef(): StateAndRef<ClientState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))

        return serviceHub.vaultService.queryBy<ClientState>(criteria = criteria).states.single()
    }

    private fun outState(): ClientState
    {
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


        return ClientState(calls,ourIdentity,counterparty,true,input.linearId)
    }

    private fun update(): TransactionBuilder
    {
        val contract = ClientContract.ID
        val notary = inputStateRef().state.notary
        val updateCommand =
                Command(ClientContract.Commands.Update(),
                        outState().participants.map { it.owningKey })

        return TransactionBuilder(notary = notary).withItems(inputStateRef(), StateAndContract(outState(), contract), updateCommand)
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

@InitiatedBy(ClientUpdateRegisterFlow::class)
class UpdateRegisterFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction" using (output is ClientState)
            }
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}

