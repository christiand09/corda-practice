package com.template.flows.register

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.RegisterContract
import com.template.states.RegisterState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.flows.FlowException
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
class UpdateFlow (private var FirstName: String,
                  private var LastName: String,
                  private var Age: String,
                  private var Gender: String,
                  private var Address: String,
                  private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>()
{
    override val progressTracker: ProgressTracker = tracker()

    companion object
    {
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
        val vault = serviceHub.vaultService.queryBy<RegisterState>(criteria = criteria).states.single()
        val input = vault.state.data
        val notary = vault.state.notary

        /* Conditions */
        // check that if the fields are empty, the data will retain its value
        if (FirstName == "")
            FirstName = input.firstName
        if (LastName == "")
            LastName = input.lastName
        if (Age == "")
            Age = input.age.toString()
        if (Gender == "")
            Gender = input.gender
        if (Address == "")
            Address = input.address

        // check that if the data is not verified, it will not update and throw a FlowException
        if (!input.approved)
            throw FlowException("The registrant must be approved before it can be update.")

        val outputState = RegisterState(FirstName, LastName, Age.toInt(), Gender, Address, input.sender, input.receiver, true, linearId = linearId)
        val updateCommand = Command(RegisterContract.Commands.Update(), listOf(ourIdentity.owningKey, input.receiver.owningKey))
        val txBuilder = TransactionBuilder(notary = notary)
                .addInputState(vault)
                .addOutputState(outputState, RegisterContract.REGISTER_ID)
                .addCommand(updateCommand)

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

@InitiatedBy(UpdateFlow::class)
class UpdateFlowResponder (val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession)
        {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a update transaction." using (output is RegisterState)
            }
        }

        val signedTransaction = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }
}