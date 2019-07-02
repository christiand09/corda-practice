package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.google.common.base.Strings.isNullOrEmpty
import com.template.contracts.MyContract
import com.template.states.MyState
import net.corda.core.contracts.Command
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
class UpdateUserFlow( private var firstName: String,
                      private var lastName: String,
                      private var age: String ,
                      private var gender: String,
                      private var address: String,
//                     private val receiver: Party,
                      private val linearId: UniqueIdentifier = UniqueIdentifier()
                    ): FlowLogic<SignedTransaction>(){


    /* Declare Transaction steps*/
    override val progressTracker = ProgressTracker(
            GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            VERIFYING_TRANSACTION,
            NOTARIZE_TRANSACTION,
            FINALISING_TRANSACTION
    )

    @Suspendable
    override fun call():SignedTransaction {

        /* Step 1 - Build the transaction */
        val inputCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val vault = serviceHub.vaultService.queryBy<MyState>(inputCriteria).states.first()
        var input = vault.state.data
        val receiver= vault.state.data.receiver

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val userState = MyState(firstName, lastName, age, gender, address, ourIdentity, input.receiver, input.approvals, input.participants, input.linearId)

       when {
           userState.firstName == "" -> userState.firstName = input.firstName
       }
        when {
            userState.lastName == "" -> userState.lastName = input.lastName
        }
        when {
            userState.gender == "" -> userState.gender = input.gender
        }
        when {
            userState.address == "" -> userState.address = input.address
        }
        when {
            userState.age == "" -> userState.age = input.age
        }

        val cmd = Command(MyContract.Commands.Issue(),ourIdentity.owningKey)
        val txBuilder = TransactionBuilder(notary)
                .addInputState(vault)
                .addOutputState(userState,MyContract.IOU_CONTRACT_ID)
                .addCommand(cmd)
        progressTracker.currentStep = GENERATING_TRANSACTION
        /* Step 2 - Verify the transaction */
        txBuilder.verify(serviceHub)
        progressTracker.currentStep = VERIFYING_TRANSACTION
        /* Step 3 - Sign the transaction */
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        val session= initiateFlow(receiver)
        progressTracker.currentStep = SIGNING_TRANSACTION
        /* Step 4 and 5 - Notarize then Record the transaction */
        progressTracker.currentStep = NOTARIZE_TRANSACTION
        progressTracker.currentStep = FINALISING_TRANSACTION
        val stx = subFlow(CollectSignaturesFlow(signedTx, listOf(session)))
        return subFlow(FinalityFlow(stx, session))
    }



}
@InitiatedBy(UpdateUserFlow::class)
class UpdateUserFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction" using (output is MyState)
            }
        }
        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}