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
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class CashIssueFlow (private val amountIssue: Long,
                     private val counterParty: String,
                     private val linearId: UniqueIdentifier) : CashFunctions()
{
    override val progressTracker = ProgressTracker(
            CREATING, VERIFYING, SIGNING, NOTARIZING, FINALIZING
    )

    @Suspendable
    override fun call(): SignedTransaction
    {
        val admin = stringToParty("PartyC")

        progressTracker.currentStep = CREATING
        val issuance = issue(admin)

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(transaction = issuance)
        val sessions = initiateFlow(stringToParty(counterParty)) // PartyB
        val adminSession = initiateFlow(admin) // PartyC
        sessions.send(true)
        adminSession.send(false)
        val transactionSignedByParties = collectSignature(transaction = signedTransaction, sessions = listOf(sessions))

        progressTracker.currentStep = NOTARIZING
        progressTracker.currentStep = FINALIZING
        return recordTransactionWithOtherParty(transaction = transactionSignedByParties, sessions = listOf(sessions, adminSession))
    }

    private fun outState(): WalletState
    {
        val input = inputStateRef(linearId).state.data

        return WalletState(
                wallet = input.wallet,
                amountIssued = amountIssue,
                amountPaid = input.amountPaid,
                status = input.status,
                borrower = ourIdentity,
                lender = stringToParty(counterParty),
                admin = input.admin,
                linearId = linearId
        )
    }

    private fun issue(PartyC: Party): TransactionBuilder =
            TransactionBuilder(notary = inputStateRef(linearId).state.notary).apply {
                val issueCommand = Command(WalletContract.Commands.Issue(), listOf(ourIdentity.owningKey, stringToParty(counterParty).owningKey))
                val stateWithAdmin = outState().copy(participants = outState().participants + PartyC)
                addInputState(inputStateRef(linearId))
                addOutputState(stateWithAdmin, WalletContract.WALLET_ID)
                addCommand(issueCommand)
            }
}

@InitiatedBy(CashIssueFlow::class)
class CashIssueFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val needsToSignTransaction = flowSession.receive<Boolean>().unwrap{ it }
        if (needsToSignTransaction)
        {
            subFlow(object : SignTransactionFlow(flowSession)
            {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an issue transaction" using (output is WalletState)
                }
            })
        }
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
    }
}


