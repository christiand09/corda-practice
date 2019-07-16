package com.template.flows.cashflows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.WalletContract
import com.template.flows.cashfunctions.*
import com.template.states.WalletState
import net.corda.core.contracts.Command
import net.corda.core.contracts.TimeWindow
import net.corda.core.flows.*
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.time.Duration
import java.time.Instant

@InitiatingFlow
@StartableByRPC
class CashRegisterFlow : CashFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val admin = stringToParty("PartyC")

        progressTracker.currentStep = CREATING
        val registration = register(admin)

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(transaction = registration)
        val sessions = emptyList<FlowSession>()
        val adminSession = initiateFlow(admin)
        val transactionSignedByParties = collectSignature(transaction = signedTransaction, sessions = sessions)

        progressTracker.currentStep = NOTARIZING
        progressTracker.currentStep = FINALIZING
        return recordTransactionWithOtherParty(transaction = transactionSignedByParties, sessions = listOf(adminSession))
    }

    private fun outState(): WalletState
    {
        val admin = stringToParty("PartyC")
        return WalletState(
                wallet = 0,
                amountIssued = 0,
                amountPaid = 0,
                status = false,
                borrower = ourIdentity,
                lender = ourIdentity,
                admin = admin
        )
    }

    private fun register(PartyC: Party): TransactionBuilder =
            TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first()).apply {
//                val ourTimeWindow = TimeWindow.fromStartAndDuration(serviceHub.clock.instant(), Duration.ofSeconds(10))
                val stateWithAdmin = outState().copy(participants = outState().participants + PartyC)
                val registerCommand = Command(WalletContract.Commands.Register(), ourIdentity.owningKey)
                addOutputState(stateWithAdmin, WalletContract.WALLET_ID)
                addCommand(registerCommand)
//                setTimeWindow(ourTimeWindow)
    }
}

@InitiatedBy(CashRegisterFlow::class)
class CashRegisterFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
    }
}