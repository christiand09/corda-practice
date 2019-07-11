package com.template.flows.cashflows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.WalletContract
import com.template.flows.cashfunctions.*
import com.template.states.WalletState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class CashRegisterFlow : FlowFunctions()
{
    override val progressTracker = ProgressTracker(
            CREATING, VERIFYING, SIGNING, NOTARIZING, FINALIZING
    )

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
        val admin = serviceHub.identityService.partiesFromName("PartyC", false).first()
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
                val stateWithAdmin = outState().copy(participants = outState().participants + PartyC)
                val registerCommand = Command(WalletContract.Commands.Register(), ourIdentity.owningKey)
                addOutputState(stateWithAdmin, WalletContract.WALLET_ID)
                addCommand(registerCommand)
    }
}

@InitiatedBy(CashRegisterFlow::class)
class RegisterFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
    }
}