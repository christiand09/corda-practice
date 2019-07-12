package com.template.flows.TokenFlows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TokenContract
import com.template.contracts.TokenContract.Companion.tokenID
import com.template.flows.*
import com.template.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap


@InitiatingFlow
@StartableByRPC
class PartyIssueFlow(private val amountIssued: Long , private val counterParty: String,private val linearId: UniqueIdentifier) : FlowFunctions() {

    override val progressTracker = ProgressTracker(
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            NOTARIZE_TRANSACTION,
            FINALISING_TRANSACTION)

    @Suspendable
    override fun call() : SignedTransaction {
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val tx = verifyAndSign(transaction(spy))

        progressTracker.currentStep = GENERATING_TRANSACTION
        progressTracker.currentStep = VERIFYING_TRANSACTION
        progressTracker.currentStep = SIGNING_TRANSACTION
        val counterRef = stringToParty(counterParty)

        val spySession = initiateFlow(spy)
        val sessions = initiateFlow(counterRef) // empty because the owner's signature is just needed

        sessions.send(true)
        spySession.send(false)
        val transactionSignedByParties = collectSignature(transaction = tx, sessions = listOf(sessions))

        progressTracker.currentStep = FINALISING_TRANSACTION
        return recordTransaction(transaction = transactionSignedByParties, sessions = listOf(sessions,spySession))
    }

    private fun outputState(): TokenState {
        val counterRef = stringToParty(counterParty)
        val input = inputStateRef(linearId).state.data
        return TokenState(amountIssued = input.amountIssued.plus(amountIssued),amountPaid = input.amountPaid,borrower = ourIdentity,lender = counterRef,iss= input.iss,walletBalance = input.walletBalance,linearId = linearId)
    }
     /****
     *SPY*
     ****/
    private fun transaction(spy: Party) =
            TransactionBuilder(notary= inputStateRef(linearId).state.notary).apply {
                val counterRef = stringToParty(counterParty)
                val spycmd = Command(TokenContract.Commands.Send(), listOf(ourIdentity.owningKey, counterRef.owningKey))
                val spiedOnMessage = outputState().copy(participants = outputState().participants + spy)
                addInputState(inputStateRef(linearId))
                addOutputState(spiedOnMessage, tokenID)
                addCommand(spycmd)
            }
    /***/
}

@InitiatedBy(PartyIssueFlow::class)
class PartyIssueFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // receive the flag
        val needsToSignTransaction = flowSession.receive<Boolean>().unwrap { it }
        // only sign if instructed to do so
        if (needsToSignTransaction) {
            subFlow(object : SignTransactionFlow(flowSession) {
                override fun checkTransaction(stx: SignedTransaction) { }
            })
        }
        // always save the transaction
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
    }
}
