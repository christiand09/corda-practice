package com.template.flows.register

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.RegisterContract
import com.template.states.RegisterState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import javax.management.Query

@InitiatingFlow
@StartableByRPC
class VerifyFlow (private val counterParty: Party,
                  private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>()
{
    override val progressTracker: ProgressTracker = tracker()

    companion object
    {
        object CREATING : ProgressTracker.Step("Creating registration!")
        object SIGNING : ProgressTracker.Step("Signing registration!")
        object VERIFYING : ProgressTracker.Step("Verifying registration!")
        object FINALISING : ProgressTracker.Step("Finalize registration!")
        {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(CREATING, SIGNING, VERIFYING, FINALISING)
    }

    @Suspendable
    override fun call(): SignedTransaction
    {
//        progressTracker.currentStep = CREATING
//        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
//        //get the information from RegisterState
//        val vault = serviceHub.vaultService.queryBy<RegisterState>(criteria = criteria).states.single()
//        val input = vault.state.data
//        val notary = vault.state.notary
//        val outputState = RegisterState(input.firstName, input.lastName, input.age, input.gender, input.address,
//                input.sender, input.receivers, approved = true, linearId = input.linearId)
//        val verifyCommand = Command(RegisterContract.Commands.Verify(), listOf(ourIdentity.owningKey, input.receivers.owningKey))
//        val txBuilder = TransactionBuilder(notary = notary)
//                .addInputState(vault)
//                .addOutputState(outputState, RegisterContract.REGISTER_ID)
//                .addCommand(verifyCommand)
//
//        progressTracker.currentStep = VERIFYING
//        txBuilder.verify(serviceHub)
//
//        progressTracker.currentStep = SIGNING
//        val ptx = serviceHub.signInitialTransaction(txBuilder)
//
//        val sessions = (outputState.participants - ourIdentity).map { initiateFlow(it) }.toSet().toList()
//        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))
//
//        progressTracker.currentStep = FINALISING
//        return subFlow(FinalityFlow(stx, sessions))
        progressTracker.currentStep = CREATING
        val verification = verify()

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(transaction = verification)
        val sessions = (outState().participants - ourIdentity).map { initiateFlow(it) }.toSet().toList()
        val transactionSignedByAllParties = collectSignature(transaction = signedTransaction, sessions = sessions)

        progressTracker.currentStep = FINALISING
        return verifyRegistration(transaction = transactionSignedByAllParties, sessions = sessions)
    }

    private fun inputStateRef(): StateAndRef<RegisterState>{
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<RegisterState>(criteria = criteria).states.single()
    }

    private fun outState(): RegisterState
    {
        val input = inputStateRef().state.data

        return RegisterState(
                input.firstName,
                input.lastName,
                input.age,
                input.gender,
                input.address,
                ourIdentity,
                counterParty,
                approved = true
        )
    }

    private fun verify(): TransactionBuilder
    {
        val notary = inputStateRef().state.notary
        val verifyCommand =
                Command(RegisterContract.Commands.Verify(),
                        listOf(ourIdentity.owningKey, counterParty.owningKey))
        val builder = TransactionBuilder(notary = notary)
        builder.addInputState(inputStateRef())
        builder.addOutputState(state = outState(), contract = RegisterContract.REGISTER_ID)
        builder.addCommand(verifyCommand)
        return builder
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

@InitiatedBy(VerifyFlow::class)
class VerifyFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession)
        {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a verify transaction" using (output is RegisterState)
            }
        }

        val signedTransaction = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }
}

