package com.template.flows.cashflows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.WalletContract
import com.template.flows.cashfunctions.*
import com.template.states.WalletState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.FlowException
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class CashSettleFlow (private val amountSettle: Long,
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
        val settleCash = settle(admin)

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(transaction = settleCash)
        val sessions = initiateFlow(stringToParty(counterParty))
        val adminSession = initiateFlow(admin)
        if (outState().status)
        {
            sessions.send(false)
            adminSession.send(true)
            val transactionSignedByParties = collectSignature(signedTransaction, listOf(adminSession))
            progressTracker.currentStep = NOTARIZING
            progressTracker.currentStep = FINALIZING
            return recordTransactionWithOtherParty(transaction = transactionSignedByParties, sessions = listOf(sessions, adminSession))
        }
        else
        {
            sessions.send(true)
            adminSession.send(false)
            val transactionSignedByParties = collectSignature(signedTransaction, listOf(sessions))
            progressTracker.currentStep = NOTARIZING
            progressTracker.currentStep = FINALIZING
            return recordTransactionWithOtherParty(transaction = transactionSignedByParties, sessions = listOf(sessions, adminSession))
        }
    }

    private fun outState(): WalletState
    {
        val input = inputStateRef(linearId).state.data

        if (amountSettle > input.amountIssued)
            throw FlowException("Amount to be settle must be less than or equal to amount issued.")

        val paid = input.amountPaid.plus(amountSettle)

        if (paid == input.amountIssued)
        {
            return WalletState(
                    wallet = input.wallet.minus(amountSettle),
                    amountIssued = input.amountIssued,
                    amountPaid = paid,
                    status = true,
                    borrower = input.borrower,
                    lender = input.lender,
                    admin = input.admin,
                    linearId = linearId
            )
        }
        else
        {
            return WalletState(
                    wallet = input.wallet.minus(amountSettle),
                    amountIssued = input.amountIssued,
                    amountPaid = paid,
                    status = false,
                    borrower = input.borrower,
                    lender = input.lender,
                    admin = input.admin,
                    linearId = linearId
            )
        }
    }

    private fun settle(PartyC: Party): TransactionBuilder =
            TransactionBuilder(notary = inputStateRef(linearId).state.notary).apply {
                val settleCommand =
                        if (!outState().status)
                            Command(WalletContract.Commands.Settle(),
                                    listOf(ourIdentity.owningKey, stringToParty(counterParty).owningKey))
                        else
                            Command(WalletContract.Commands.Settle(), listOf(PartyC.owningKey))

                if (!outState().status)
                        {
                            val stateWithAdmin = outState().copy(participants = outState().participants + PartyC)
                            addInputState(inputStateRef(linearId))
                            addOutputState(stateWithAdmin, WalletContract.WALLET_ID)
                            addCommand(settleCommand)
                        }

                        else
                        {
                            val stateWithAdmin = outState().copy(participants = outState().participants + PartyC - outState().borrower - outState().lender)
                            addInputState(inputStateRef(linearId))
                            addOutputState(stateWithAdmin, WalletContract.WALLET_ID)
                            addCommand(settleCommand)
                        }
            }
}

@InitiatedBy(CashSettleFlow::class)
class CashSettleFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
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
                    "This must be a settle transaction" using (output is WalletState)
                }
            })
        }
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
    }
}