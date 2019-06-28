package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.ClientContract
import com.template.states.ClientState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker


@InitiatingFlow
@StartableByRPC
class ClientRegisterFlow(private val name: String,
                         private val age: Int,
                         private val receiver: Party,
                         private val sender: Party) : FlowLogic<SignedTransaction>() {

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
    override fun call(): SignedTransaction {/* Step 1 - Build the transaction */
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val userState = ClientState(name,age,receiver,sender,false)
        val cmd = Command(ClientContract.Commands.Register(),ourIdentity.owningKey)


        val txBuilder = TransactionBuilder(notary)
                .addOutputState(userState,ClientContract.ID)
                .addCommand(cmd)
        progressTracker.currentStep = BUILDING_TRANSACTION

        /* Step 2 - Verify the transaction */
        txBuilder.verify(serviceHub)
        progressTracker.currentStep = VERIFY_TRANSACTION

        /* Step 3 - Sign the transaction */
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        val sessions= initiateFlow(receiver)
        progressTracker.currentStep = SIGN_TRANSACTION



        /* Step 4 and 5 - Notarize then Record the transaction */
        progressTracker.currentStep = NOTARIZE_TRANSACTION
        progressTracker.currentStep = RECORD_TRANSACTION
        return subFlow(FinalityFlow(signedTx, sessions))
    }
}


/**
 * This is the flow which signs IOU issuances.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(ClientRegisterFlow::class)
class ClientFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

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
