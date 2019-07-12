package com.template.flows.cashflows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.WalletContract
import com.template.flows.cashfunctions.*
import com.template.states.WalletState
import net.corda.core.contracts.*
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class CashSelfIssueFlow (private val selfIssueAmount: Long,
                         private val linearId: UniqueIdentifier): CashFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val admin = stringToParty("PartyC")

        progressTracker.currentStep = CREATING
        val selfIssuance = selfIssue(admin)

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(transaction = selfIssuance)
        val sessions = emptyList<FlowSession>()
        val adminSession = initiateFlow(admin)
        val transactionSignedByParties = collectSignature(transaction = signedTransaction, sessions = sessions)

        progressTracker.currentStep = NOTARIZING
        progressTracker.currentStep = FINALIZING
        return recordTransactionWithOtherParty(transaction = transactionSignedByParties, sessions = listOf(adminSession))
    }

    private fun outState(): WalletState
    {
        val input = inputStateRef(linearId).state.data
        return WalletState(
                wallet = input.wallet.plus(selfIssueAmount),
                amountIssued = input.amountIssued,
                amountPaid = input.amountPaid,
                status = input.status,
                borrower = ourIdentity,
                lender = ourIdentity,
                admin = input.admin,
                linearId = linearId
        )
    }

    private fun selfIssue(PartyC: Party): TransactionBuilder =
            TransactionBuilder(notary = inputStateRef(linearId).state.notary).apply{
                val selfIssueCommand = Command(WalletContract.Commands.SelfIssue(), ourIdentity.owningKey)
                val stateWithAdmin = outState().copy(participants = outState().participants + PartyC)
                addInputState(inputStateRef(linearId))
                addOutputState(stateWithAdmin, WalletContract.WALLET_ID)
                addCommand(selfIssueCommand)
    }
}

@InitiatedBy(CashSelfIssueFlow::class)
class CashSelfIssueFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
    }
}