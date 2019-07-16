package com.template.flows.cashflows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.WalletContract
import com.template.flows.cashfunctions.*
import com.template.states.WalletState
import net.corda.core.contracts.Command
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import java.time.Duration
import java.time.Instant

@InitiatingFlow
@StartableByRPC
class CashTransferFlow (private val amountTransfer: Long,
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
        val transferCash = transfer(admin)

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(transaction = transferCash)
        val sessions = initiateFlow(stringToParty(counterParty))
        val adminSession = initiateFlow(admin)
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
                wallet = input.wallet.plus(amountTransfer),
                amountIssued = input.amountIssued,
                amountPaid = input.amountPaid,
                status = input.status,
                borrower = input.borrower,
                lender = input.lender,
                admin = input.admin,
                linearId = linearId
        )
    }

    private fun transfer(PartyC: Party): TransactionBuilder =
            TransactionBuilder(notary = inputStateRef(linearId).state.notary).apply {
                val transferCommand = Command(WalletContract.Commands.Transfer(), listOf(ourIdentity.owningKey, stringToParty(counterParty).owningKey))
                val stateWithAdmin = outState().copy(participants = outState().participants + PartyC)
                val timeWindow = TimeWindow.between(Instant.now(), Instant.now() + Duration.ofSeconds(5))
                addInputState(inputStateRef(linearId))
                addOutputState(stateWithAdmin, WalletContract.WALLET_ID)
                addCommand(transferCommand)
                setTimeWindow(timeWindow)
            }
}

@InitiatedBy(CashTransferFlow::class)
class CashTransferFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
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
                    "This must be a transfer transaction" using (output is WalletState)
                }
            })
        }
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
    }
}