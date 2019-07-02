package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.flows.progressTracker.BUILDING
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

@InitiatingFlow
class BroadcastTransactionFlow(
        private val stx: SignedTransaction,
        private val recipients: List<Party>
) : FlowLogic<Unit>() {

    companion object {
        object STARTING : Step("Starting Broadcast.")
        object CREATING :  Step("Creating Sessions.")
        object SENDING :  Step("Start subFlow(SendTransaction).")
        object RECEIVING :  Step("Start subFlow(ReceiveTransaction).")

        fun tracker() = ProgressTracker(STARTING, CREATING, SENDING, RECEIVING)
    }
    override val progressTracker: ProgressTracker = tracker()


    @Suspendable
    override fun call() {

        progressTracker.currentStep = STARTING
        for (recipient in recipients) {
            progressTracker.currentStep = CREATING
            val session = initiateFlow(recipient)
            progressTracker.currentStep = SENDING
            subFlow(SendTransactionFlow(session, stx))
        }
    }
}

@InitiatedBy(BroadcastTransactionFlow::class)
class BroadcastTransactionResponder(private val session: FlowSession) : FlowLogic<Unit>() {

    override val progressTracker: ProgressTracker = BroadcastTransactionFlow.tracker()

    @Suspendable
    override fun call() {
        progressTracker.currentStep = BroadcastTransactionFlow.Companion.RECEIVING
        subFlow(ReceiveTransactionFlow(session, statesToRecord = StatesToRecord.ALL_VISIBLE))
    }
}