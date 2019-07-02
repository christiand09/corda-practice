package com.template.flows.register

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.RegisterContract
import com.template.states.Name
import com.template.states.RegisterState
import net.corda.core.contracts.*
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.*
import net.corda.core.flows.FlowException
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class UpdateFlow (private var name: Name,
                  private val counterParty: Party,
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
        val updating = update()

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(transaction = updating)
        val sessions = (outState().participants - ourIdentity).map { initiateFlow(it) }.toSet().toList()
        val transactionSignedByAllParties = collectSignature(transaction = signedTransaction, sessions = sessions)

        progressTracker.currentStep = FINALISING
//        subFlow(RegisterFlow(FirstName, LastName, Age.toInt(), Gender, Address))
        return verifyRegistration(transaction = transactionSignedByAllParties, sessions = sessions)
    }

    private fun inputStateRef(): StateAndRef<RegisterState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<RegisterState>(criteria = criteria).states.single()
    }

    private fun outState(): RegisterState
    {
        val input = inputStateRef().state.data

        if (name.firstname == "")
            name.firstname = input.name.firstname
        if (name.lastname == "")
            name.lastname = input.name.lastname
        if (name.age == "")
            name.age = input.name.age
        if (name.gender == "")
            name.gender = input.name.gender
        if (name.address == "")
            name.address = input.name.address

        if (!input.approved)
            throw FlowException("The registrant must be approved before it can be update.")

        return RegisterState(
                name,
                ourIdentity,
                counterParty,
                approved = true,
                linearId = linearId
        )
    }

    private fun update(): TransactionBuilder
    {
        val contract = RegisterContract.REGISTER_ID
        val notary = inputStateRef().state.notary
        val updateCommand =
                Command(RegisterContract.Commands.Update(),
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