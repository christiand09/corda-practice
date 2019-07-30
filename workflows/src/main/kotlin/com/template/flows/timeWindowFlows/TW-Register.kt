package com.template.flows.timeWindowFlows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import com.template.contracts.MyContract
import com.template.flows.*
import com.template.states.TimeWindowState
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap


@InitiatingFlow
@StartableByRPC
class TWRegister(): FlowFunction() {
    override val progressTracker = ProgressTracker(INITIALIZING, BUILDING, SIGNING, COLLECTING, FINALIZING)

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = INITIALIZING
        val spy = stringToPartySpy("PartyC")
        val spySession = initiateFlow(spy)
        spySession.send(false)
        val transaction: TransactionBuilder = transaction(spy)
        val signedTransaction: SignedTransaction = verifyAndSign(transaction)
        val sessions = emptyList<FlowSession>()
        val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, sessions)
        return recordTransaction(transactionSignedByAllParties, listOf(spySession))
    }

    private fun outputState(): TimeWindowState
    {val spy = stringToPartySpy("PartyC")
        return TimeWindowState(
                status = false,
                sender = ourIdentity,
                receiver = ourIdentity,
                spy = spy
        )
    }

    private fun transaction(spy:Party): TransactionBuilder {
        progressTracker.currentStep = BUILDING
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val issueCommand = Command(MyContract.Commands.Issue(),ourIdentity.owningKey)
        val spiedOnMessage = outputState().copy(participants = listOf(ourIdentity, spy))
        val builder = TransactionBuilder(notary = notary )
        builder.addOutputState(spiedOnMessage, MyContract.IOU_CONTRACT_ID)
        builder.addCommand(issueCommand)
        return builder
    }
}

@InitiatedBy(TWRegister::class)
class TWRegisterResponder(private val sessions: FlowSession) : FlowLogic<SignedTransaction>() {

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