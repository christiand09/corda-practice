package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.ClientContract
import com.template.states.ClientState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class VerifyFlow (private  val Id: UniqueIdentifier): FlowLogic<SignedTransaction>(){

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

        /* Step 1 - Build the transaction */
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(Id))

        val inputState = serviceHub.vaultService.queryBy<ClientState>(criteria).states.single()

        val input = inputState.state.data
        val receiver = inputState.state.data.receiver


        val clientState = ClientState(input.name,input.age,input.receiver,input.sender,true)

        val cmd = Command(ClientContract.Commands.Verify(), listOf(receiver.owningKey,ourIdentity.owningKey))

        val txBuilder = TransactionBuilder(notary)
                .addInputState(inputState)
                .addOutputState(clientState,ClientContract.ID)
                .addCommand(cmd)
        progressTracker.currentStep = BUILDING_TRANSACTION


        /* Step 2 - Verify the transaction */
        progressTracker.currentStep = VERIFY_TRANSACTION
        txBuilder.verify(serviceHub)


        /* Step 3 - Sign the transaction */
        progressTracker.currentStep = SIGN_TRANSACTION
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        val session = initiateFlow(receiver)



        val stx = subFlow(CollectSignaturesFlow(signedTx, listOf(session)))

        /* Step 4 and 5 - Notarize then Record the transaction */
        progressTracker.currentStep = NOTARIZE_TRANSACTION
        progressTracker.currentStep = RECORD_TRANSACTION
        return subFlow(FinalityFlow(stx, session))


    }
}

@InitiatedBy(VerifyFlow::class)
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