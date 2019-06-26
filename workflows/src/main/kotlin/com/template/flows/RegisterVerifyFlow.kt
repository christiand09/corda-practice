package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.RegisterContract
import com.template.states.RegisterState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class RegisterVerifyFlow (private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>()
{
    override val progressTracker: ProgressTracker = tracker()

    companion object {
        object CREATING : ProgressTracker.Step("Creating registration!")
        object SIGNING : ProgressTracker.Step("Signing registration!")
        object VERIFYING : ProgressTracker.Step("Verifying registration!")
        object FINALISING : ProgressTracker.Step("Finalize registration!") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(CREATING, SIGNING, VERIFYING, FINALISING)
    }

    @Suspendable
    override fun call(): SignedTransaction
    {
        progressTracker.currentStep = CREATING
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        //get the information from KYCState
        val vault = serviceHub.vaultService.queryBy<RegisterState>(criteria).states.single()
        val input = vault.state.data
        val notary = vault.state.notary
        val outputState = RegisterState(input.firstName, input.lastName, input.age, input.gender, input.address,
                input.sender, input.receiver, approved = true)
        val verifyCommand = Command(RegisterContract.Commands.Verify(), listOf(input.receiver.owningKey, ourIdentity.owningKey))
        val txBuilder = TransactionBuilder(notary = notary)
                .addInputState(vault)
                .addOutputState(outputState, RegisterContract.REGISTER_ID)
                .addCommand(verifyCommand)

        progressTracker.currentStep = VERIFYING
        txBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING
        val ptx = serviceHub.signInitialTransaction(txBuilder)
        val targetSession = initiateFlow(input.receiver)
        val sessions = listOf(targetSession)
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))

        progressTracker.currentStep = FINALISING
        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(RegisterVerifyFlow::class)
class RegisterVerifyFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction" using (output is RegisterState)
            }
        }

        val signedTransaction = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }
}

