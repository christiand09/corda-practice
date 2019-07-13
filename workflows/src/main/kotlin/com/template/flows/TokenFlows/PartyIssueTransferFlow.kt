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
class PartyIssueTransferFlow(private val amountToLend: Long,
                             private val counterParty: String,
                             private val linearId: UniqueIdentifier) : FlowFunctions() {

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = GENERATING_TRANSACTION
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val tx = verifyAndSign(transaction(spy))


//        val amountTransfer = PartyTransfer()
        progressTracker.currentStep = VERIFYING_TRANSACTION
        progressTracker.currentStep = SIGNING_TRANSACTION
//        val signedTransaction = verifyAndSign(transaction = amountTransfer)

        val counterRef = stringToParty(counterParty)
        val sessions = initiateFlow(counterRef) // empty because the owner's signature is just needed
        val spySession = initiateFlow(spy)

        sessions.send(true)
        spySession.send(false)

        val transactionSignedByParties = collectSignature(transaction = tx, sessions = listOf(sessions))


        progressTracker.currentStep = FINALISING_TRANSACTION
        return recordTransaction(transaction = transactionSignedByParties, sessions = listOf(sessions, spySession))
    }

    private fun outputState(): TokenState {
        val counterRef = stringToParty(counterParty)
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val input = inputStateRef(linearId).state.data

//        return TokenState(amount = amount,borrower = ourIdentity,lender = counterRef,walletbalance = input.walletbalance,linearId = linearId)
        return TokenState(amountIssued = input.amountIssued,
                amountPaid = input.amountPaid,
                borrower = counterRef,
                lender = ourIdentity,
                iss = spy,
                walletBalance = input.walletBalance.plus(amountToLend),
                settled = input.settled,
                linearId = linearId)
    }

    /****
     *SPY*
     ****/
    private fun transaction(spy: Party) =
            TransactionBuilder(notary = inputStateRef(linearId).state.notary).apply {
                val counterRef = stringToParty(counterParty)
                val spycmd = Command(TokenContract.Commands.Send(), listOf(ourIdentity.owningKey, counterRef.owningKey))
                // the spy is added to the messages participants
                val spiedOnMessage = outputState().copy(participants = outputState().participants + spy)
                addInputState(inputStateRef(linearId))
                addOutputState(spiedOnMessage, tokenID)

                addCommand(spycmd)
            }
    /***/

}


@InitiatedBy(PartyIssueTransferFlow::class)
class PartyIssueTransferFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // receive the flag
        val needsToSignTransaction = flowSession.receive<Boolean>().unwrap { it }
        // only sign if instructed to do so
        if (needsToSignTransaction) {
            subFlow(object : SignTransactionFlow(flowSession) {
                override fun checkTransaction(stx: SignedTransaction) {}
            })
        }
        // always save the transaction
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
    }
}
