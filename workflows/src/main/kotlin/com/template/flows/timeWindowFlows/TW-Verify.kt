package com.template.flows.timeWindowFlows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.MyContract
import com.template.flows.*
import com.template.states.MyState
import com.template.states.TimeWindowState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.minutes
import net.corda.core.utilities.seconds
import net.corda.core.utilities.unwrap
import java.time.Duration
import java.time.Instant

/**
 * This is the flow which handles issuance of new IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@InitiatingFlow
@StartableByRPC
class TWVerify(val receiver: String,
               val linearId: UniqueIdentifier = UniqueIdentifier()): FlowFunction() {
    override val progressTracker = ProgressTracker(INITIALIZING, BUILDING, SIGNING, COLLECTING, FINALIZING)

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = INITIALIZING
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
    }



    private fun outputState(): TimeWindowState {
        val input = iState(linearId).state.data
        val spy = stringToPartySpy("PartyC")
        val counterRef = stringToParty(receiver)
        return TimeWindowState(
                status = true,
                sender = ourIdentity,
                receiver = counterRef,
                spy = spy,
                linearId = linearId,
                participants = listOf(ourIdentity, counterRef, input.spy)
        )
    }

    private fun transaction(): TransactionBuilder {
        progressTracker.currentStep = BUILDING
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val spy = stringToPartySpy("PartyC")
        val cmd = Command(MyContract.Commands.Verify(), (outputState().participants - spy).map { it.owningKey })
//          from start-time plus a duration:
//        val timeWindow = TimeWindow.fromStartAndDuration(serviceHub.clock.instant(), 5.seconds)
//        from only after [add plusSeconds(30) at the return]:
        val timeWindow = TimeWindow.fromOnly(getTime(linearId))
        val builder = TransactionBuilder(notary = notary)
        builder.addInputState(iState(linearId))
        builder.addOutputState(outputState(), MyContract.IOU_CONTRACT_ID)
        builder.addCommand(cmd)
        builder.setTimeWindow(timeWindow)
        return builder
    }
}
/**
 * This is the flow which signs IOU issuances.
 * The signing is handled by the [SignTransactionFlow].
 */

@InitiatedBy(TWVerify::class)
class TWVerifyResponder(private val sessions: FlowSession) : FlowLogic<SignedTransaction>() {

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