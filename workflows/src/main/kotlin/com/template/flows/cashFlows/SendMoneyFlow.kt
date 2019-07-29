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
class SendMoneyFlow(private val receiver: String,
                    private val amount: Int,
                    private val linearId: UniqueIdentifier = UniqueIdentifier()): FlowFunction() {
    override val progressTracker = ProgressTracker(INITIALIZING, BUILDING, SIGNING, COLLECTING, FINALIZING)

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = INITIALIZING
        if (!outputState().approvals){
            throw IllegalArgumentException("${outputState().formSet.firstName} is not yet a verified user.")
        }
        else
        {
        val spy = stringToPartySpy("PartyC")
        val spySession = initiateFlow(spy)
        spySession.send(false)
        val transaction: TransactionBuilder = transaction()
        val signedTransaction: SignedTransaction = verifyAndSign(transaction)
        val counterRef = stringToParty(receiver)
        val sessions = initiateFlow(counterRef)
        sessions.send(true)
        val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, listOf(sessions))
        return recordTransaction(transactionSignedByAllParties, listOf(sessions, spySession))
    }}

    private fun outputState(): MyState {
        val spy = stringToPartySpy("PartyC")
        val input = inputStateRef(linearId).state.data
        val counterRef = stringToParty(receiver)
        return MyState(
                formSet =  input.formSet,
                sender = ourIdentity,
                receiver =counterRef,
                spy = input.spy,
                wallet = input.wallet + amount,
                amountdebt = input.amountdebt + amount,
                amountpaid = input.amountpaid,
                status = "Sending $amount to $counterRef",
                debtFree = false,
                approvals = input.approvals,
                linearId = linearId,
                participants = listOf(ourIdentity, counterRef, spy)

        )
    }

    private fun transaction(): TransactionBuilder {
        progressTracker.currentStep = BUILDING
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val spy = stringToPartySpy("PartyC")
        val cmd = Command(MyContract.Commands.Verify(), (outputState().participants - spy).map { it.owningKey })
        val builder = TransactionBuilder(notary = notary)
        builder.addInputState(inputStateRef(linearId))
        builder.addOutputState(outputState(), MyContract.IOU_CONTRACT_ID)
        builder.addCommand(cmd)
        return builder
    }
}

/**
 * This is the flow which signs IOU issuances.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(SendMoneyFlow::class)
class SendMoneyFlowFlowResponder(private val sessions: FlowSession) : FlowLogic<SignedTransaction>() {

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