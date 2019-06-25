package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.RegisterContract
import com.template.states.RegisterState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

@InitiatingFlow
@StartableByRPC
class RegisterFlow (private val FirstName : String,
                    private val LastName : String,
                    private val Age : Int,
                    private val Gender : String,
                    private val Address : String,
                    private val counterParty: Party) : FlowLogic<SignedTransaction>()
{
    override val progressTracker: ProgressTracker = tracker()

    companion object {
        object CREATING : Step("Creating registration!")
        object SIGNING : Step("Signing registration!")
        object VERIFYING : Step("Verifying registration!")
        object FINALISING : Step("Finalize registration!") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(CREATING, SIGNING, VERIFYING, FINALISING)
    }

    @Suspendable
    override fun call(): SignedTransaction {

        progressTracker.currentStep = CREATING
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val outputState = RegisterState(ourIdentity, FirstName, LastName, Age, Gender, Address, counterParty)
        val command = Command(RegisterContract.Commands.Register(), ourIdentity.owningKey)
        val txBuilder = TransactionBuilder(notary = notary)
                .addOutputState(outputState, RegisterContract.ID)
                .addCommand(command)

        progressTracker.currentStep = VERIFYING
        txBuilder.verify(services = serviceHub)

        progressTracker.currentStep = SIGNING
        val ptx = serviceHub.signInitialTransaction(txBuilder)
        val targetSession = initiateFlow(counterParty)
        val sessions = listOf(targetSession)
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))

        progressTracker.currentStep = FINALISING
        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(RegisterFlow::class)
class RegisterFlowResponder (val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession)
        {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a register transaction" using (output is RegisterState)
            }
        }
        val signedTransaction = subFlow(signTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = counterpartySession, expectedTxId = signedTransaction.id))
    }
}

