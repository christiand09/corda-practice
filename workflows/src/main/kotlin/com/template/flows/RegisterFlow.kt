package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.RegisterContract
import com.template.states.RegisterState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class RegisterFlow (val FirstName : String,
                    val LastName : String,
                    val Age : Int,
                    val Gender : String,
                    val Address : String,
                    val IsApproved : Boolean) : FlowLogic<SignedTransaction>()
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
    override fun call(): SignedTransaction {
        progressTracker.currentStep = CREATING

        val me = serviceHub.myInfo.legalIdentities.first()
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val outputState = RegisterState(ourIdentity, FirstName, LastName, Age, Gender, Address, IsApproved, listOf(ourIdentity))
        val command = Command(RegisterContract.Commands.Register(), listOf(me.owningKey))
        val txBuilder = TransactionBuilder(notary = notary)
                .addOutputState(outputState, RegisterContract.ID)
                .addCommand(command)

        progressTracker.currentStep = SIGNING
        val stx = serviceHub.signInitialTransaction(txBuilder)

        progressTracker.currentStep = VERIFYING
        stx.verify(serviceHub)

        progressTracker.currentStep = FINALISING
        val targetSession = initiateFlow(ourIdentity)
        val sessions = listOf(targetSession)
        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(RegisterFlow::class)
class RegisterFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction {
        TODO("to be implemented")
    }
}