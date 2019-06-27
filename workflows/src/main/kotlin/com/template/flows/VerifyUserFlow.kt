package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.MyContract
import com.template.states.MyState
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
class VerifyUserFlow (private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>(){

    override val progressTracker = ProgressTracker(
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            NOTARIZE_TRANSACTION,
            FINALISING_TRANSACTION )

    @Suspendable
    override fun call() : SignedTransaction {
        progressTracker.currentStep = GENERATING_TRANSACTION
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))


        val vault = serviceHub.vaultService.queryBy<MyState>(criteria).states.single()
        val input = vault.state.data
        val receiver= vault.state.data.receiver
        val notary = vault.state.notary
        val outputState = MyState(input.firstName,input.lastName,input.age,input.gender,input.address,input.sender,input.receiver,true  )



        val cmd = Command(MyContract.Commands.Issue(),listOf(receiver.owningKey, ourIdentity.owningKey))
        val txBuilder = TransactionBuilder(notary= notary)
                .addInputState(vault)
                .addOutputState(outputState, MyContract.IOU_CONTRACT_ID)
                .addCommand(cmd)

        progressTracker.currentStep = VERIFYING_TRANSACTION
        txBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        val session= initiateFlow(receiver)

        progressTracker.currentStep = NOTARIZE_TRANSACTION
        progressTracker.currentStep = FINALISING_TRANSACTION
        val stx = subFlow(CollectSignaturesFlow(signedTx, listOf(session)))
        return subFlow(FinalityFlow(stx,session))
    }

    @InitiatedBy(VerifyUserFlow::class)
    class VerifyFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {
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

}