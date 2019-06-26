package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.contracts.UserContract.Companion.REGISTER_ID
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.lang.IllegalArgumentException
import java.security.acl.Owner

@InitiatingFlow
@StartableByRPC
class VerifyFlow (
                   private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>(){


    override val progressTracker = ProgressTracker(
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            NOTARIZE_TRANSACTION,
            FINALISING_TRANSACTION )

    @Suspendable
    override fun call() : SignedTransaction {

        progressTracker.currentStep = GENERATING_TRANSACTION
        // verify notary


        //Search in servicehub in the map all the parties listed and change the string in a Party
//        val OwnerRef = serviceHub.identityService.partiesFromName(OwnParty, false).singleOrNull()
//                ?: throw IllegalArgumentException("No match found for Owner $OwnParty.")

        // Initiator flow logic goes here.
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        //get the information from KYCState
        val Vault = serviceHub.vaultService.queryBy<UserState>(criteria).states.single()
        val input = Vault.state.data
        val receiver= Vault.state.data.receiver
        val notary = Vault.state.notary
        // belong to the transaction
        val outputState = UserState(input.firstName,input.lastName,input.age,input.gender,input.address,input.sender,input.receiver,true  )



//        val state = UserState(input.firstName,input.lastName,input.age,input.gender,input.address,input.sender,input.receiver,true )
        // valid or invalid in contract
        val cmd = Command(UserContract.Commands.Verify(),listOf(receiver.owningKey, ourIdentity.owningKey))

        //add transaction Builder
        val txBuilder = TransactionBuilder(notary= notary)
                .addInputState(Vault)
                .addOutputState(outputState, REGISTER_ID)
                .addCommand(cmd)

        progressTracker.currentStep = VERIFYING_TRANSACTION
        //verification of transaction
        txBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        //signed by the participants
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        val session= initiateFlow(receiver)




        progressTracker.currentStep = NOTARIZE_TRANSACTION
        progressTracker.currentStep = FINALISING_TRANSACTION
        //Notarize then Record the transaction
        val stx = subFlow(CollectSignaturesFlow(signedTx, listOf(session)))
        return subFlow(FinalityFlow(stx,session))
    }




    @InitiatedBy(VerifyFlow::class)
    class VerifyFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an IOU transaction" using (output is UserState)
                }
            }

            val txWeJustSignedId = subFlow(signedTransactionFlow)

            return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
        }
    }
}