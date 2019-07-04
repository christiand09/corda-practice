package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.ClientContract
import com.template.contracts.ClientContract.Companion.ID
import com.template.states.ClientState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class ClientVerifyFlow (private val counterparty: Party, private  val linearId: UniqueIdentifier): FlowLogic<SignedTransaction>(){

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
//    @Suspendable
//    override fun call(): SignedTransaction {
//
//        /* Step 1 - Build the transaction */
//        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//
//        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
//
//        val inputState = serviceHub.vaultService.queryBy<ClientState>(criteria).states.single()
//
//        val input = inputState.state.data
//
//
//
//
//        val clientState = ClientState(input.name,input.age,input.address,input.birthDate,input.status,input.religion,input.sender,counterparty,true,input.linearId)
//
//
//        val cmd = Command(ClientContract.Commands.Verify(), listOf(ourIdentity.owningKey, counterparty.owningKey))
//
//        val txBuilder = TransactionBuilder(notary)
//                .addInputState(inputState)
//                .addOutputState(clientState,ClientContract.ID)
//                .addCommand(cmd)
//        progressTracker.currentStep = BUILDING_TRANSACTION
//
//
//        /* Step 2 - Verify the transaction */
//        progressTracker.currentStep = VERIFY_TRANSACTION
//        txBuilder.verify(serviceHub)
//
//
//        /* Step 3 - Sign the transaction */
//        progressTracker.currentStep = SIGN_TRANSACTION
//        val signedTx = serviceHub.signInitialTransaction(txBuilder)
//
//        val sessions = initiateFlow(counterparty)
//
//
//        // Step 9. Collecting missing signatures
//        val stx = subFlow(CollectSignaturesFlow(signedTx, listOf(sessions)))
//
//        /* Step 4 and 5 - Notarize then Record the transaction */
//        progressTracker.currentStep = NOTARIZE_TRANSACTION
//        progressTracker.currentStep = RECORD_TRANSACTION
//        return subFlow(FinalityFlow(stx, sessions))
//
//
//    }
//}

    @Suspendable
    override fun call(): SignedTransaction {
       val unVerified = userVerified()
        val signedTransaction = verifyAndSign(unVerified)
        val sessions=(outputState().participants-ourIdentity).map {initiateFlow(it)}.toList()
        val transactionSignedByAllParties: SignedTransaction=collectSignature(signedTransaction, sessions)
        return recordTransaction(transactionSignedByAllParties, sessions)
    }
    private fun inputStateRef(): StateAndRef<ClientState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<ClientState>(criteria).states.single()
    }
    private fun outputState():ClientState{
        val input = inputStateRef().state.data
        return ClientState(input.calls,input.sender,counterparty,true,input.linearId)
    }
    private fun userVerified(): TransactionBuilder{
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val cmd = Command ( ClientContract.Commands.Register(), outputState().participants.map {it.owningKey})
        return TransactionBuilder(notary)
                .addOutputState(outputState(), ID)
                .addCommand(cmd)
    }
    private fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction{
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



@InitiatedBy(ClientVerifyFlow::class)
class VerifyFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

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