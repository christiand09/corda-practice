package com.template.flows.cashFlows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.MyContract
import com.template.flows.*
import com.template.states.MyState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

/**
 * This is the flow which handles issuance of new IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@InitiatingFlow
@StartableByRPC
class PayDebtFlow(private val amountToPay: Int,
                  private val receiver: String,
                  private val linearId: UniqueIdentifier = UniqueIdentifier()): FlowFunction() {
    override val progressTracker = ProgressTracker(INITIALIZING, BUILDING, SIGNING, COLLECTING, FINALIZING)

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = INITIALIZING
        if (!outputState().approvals){
            throw IllegalArgumentException("${outputState().formSet.firstName} is not yet a verified user.")
        }
        else {
            if (outputState().debtFree) {
                val spy = stringToPartySpy("PartyC")
                val spySession = initiateFlow(spy)
                spySession.send(true)
                val counterRef = stringToParty(receiver)
                val sessions = initiateFlow(counterRef)
                sessions.send(false)
                val transaction: TransactionBuilder = transaction()
                val signedTransaction: SignedTransaction = verifyAndSign(transaction)
                val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, listOf(spySession))
                return recordTransaction(transactionSignedByAllParties, listOf(sessions, spySession))}
            else {
                val spy = stringToPartySpy("PartyC")
                val spySession = initiateFlow(spy)
                spySession.send(false)
                val counterRef = stringToParty(receiver)
                val sessions = initiateFlow(counterRef)
                sessions.send(true)
                val transaction: TransactionBuilder = transaction()
                val signedTransaction: SignedTransaction = verifyAndSign(transaction)
                val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, listOf(sessions))
                return recordTransaction(transactionSignedByAllParties, listOf(sessions, spySession))
            }

            }
    }

    private fun outputState(): MyState {
        val input = inputStateRef(linearId).state.data
        val paid = input.amountpaid + amountToPay
        val counterRef = stringToParty(receiver)
        return if (paid == input.amountdebt)
        {
            MyState(
                    formSet =  input.formSet,
                    sender = ourIdentity,
                    receiver = counterRef,
                    spy = input.spy,
                    wallet = input.wallet - amountToPay,
                    amountdebt = input.amountdebt,
                    amountpaid = input.amountpaid + amountToPay,
                    status = "User's debt is paid in full.",
                    debtFree = true,
                    approvals = input.approvals,
                    linearId = linearId
//                    participants = outputState().participants + input.spy - outputState().receiver - outputState().sender
            )
        }
        else
        {
            MyState(
                    formSet =  input.formSet,
                    sender = ourIdentity,
                    receiver = counterRef,
                    spy = input.spy,
                    wallet = input.wallet - amountToPay,
                    amountdebt = input.amountdebt,
                    amountpaid = input.amountpaid + amountToPay,
                    status = "User is paying $amountToPay to $counterRef",
                    debtFree = false,
                    approvals = input.approvals,
                    linearId = linearId
//                    participants = listOf(ourIdentity, counterRef, input.spy)
            )
        }}

    private fun transaction(): TransactionBuilder {
        progressTracker.currentStep = BUILDING
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val spy = stringToPartySpy("PartyC")
        val counterRef = stringToParty(receiver)
        val transactionCommandd =
                if (!outputState().debtFree) {
                    Command(MyContract.Commands.Issue(), listOf(ourIdentity.owningKey, counterRef.owningKey))
                } else {
                    Command(MyContract.Commands.Issue(), listOf(spy.owningKey))
                }
        if (!outputState().debtFree) {
            val spiedOnMessage = outputState().copy(participants = outputState().participants + spy)
            val builder = TransactionBuilder(notary = notary)
            builder.addInputState(inputStateRef(linearId))
            builder.addOutputState(spiedOnMessage, MyContract.IOU_CONTRACT_ID)
            builder.addCommand(transactionCommandd)
            return builder

        } else {
            val spiedOnMessage = outputState().copy(participants = outputState().participants + spy - outputState().sender - outputState().receiver)
            val builder = TransactionBuilder(notary = notary)
            builder.addInputState(inputStateRef(linearId))
            builder.addOutputState(spiedOnMessage, MyContract.IOU_CONTRACT_ID)
            builder.addCommand(transactionCommandd)
            return builder
        }
    }
}
/**
 * This is the flow which signs IOU issuances.
 * The signing is handled by the [SignTransactionFlow].
 */


@InitiatedBy(PayDebtFlow::class)
class PayDebtFlowResponder(private val sessions: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // receive the flag
        val needsToSignTransaction = sessions.receive<Boolean>().unwrap { it }
        // only sign if instructed to do so
        if (needsToSignTransaction) {
            subFlow(object : SignTransactionFlow(sessions) {
                override fun checkTransaction(stx: SignedTransaction) { }
            })
        }
        // always save the transaction
        return subFlow(ReceiveFinalityFlow(otherSideSession = sessions))
    }
}