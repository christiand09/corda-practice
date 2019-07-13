package com.template.flows.TokenFlows
import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TokenContract
import com.template.flows.*
import com.template.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
//private val spy: String
class UserWithTokenRegisterFlow : FlowFunctions() {

    @Suspendable
    override fun call() : SignedTransaction {
        progressTracker.currentStep = GENERATING_TRANSACTION
//        val admin = stringToParty(spy)
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val tx = verifyAndSign(transaction(spy))

        val sessions = emptyList<FlowSession>()
        val spySession = initiateFlow(spy)

        progressTracker.currentStep = VERIFYING_TRANSACTION
        progressTracker.currentStep = SIGNING_TRANSACTION
        val transactionSignedByParties = collectSignature(transaction = tx, sessions = sessions)
//        sessions.send(true)
        spySession.send(false)
        progressTracker.currentStep = FINALISING_TRANSACTION
        return recordTransaction(transaction = transactionSignedByParties, sessions = listOf(spySession))
    }

    private fun outputState(): TokenState {
//        val admin = stringToParty(spy)
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        return TokenState(amountIssued = 0,amountPaid = 0,borrower = ourIdentity,lender = ourIdentity,iss= spy,walletBalance = 0)
    }
    private fun transaction(spy: Party) = TransactionBuilder(notary= serviceHub.networkMapCache.notaryIdentities.first()).apply {
//                val admin = stringToParty(spy)
                val spycmd = Command(TokenContract.Commands.Send(), ourIdentity.owningKey)
                // the spy is added to the messages participants
                val spiedOnMessage = outputState().copy(participants = outputState().participants + spy)
                addOutputState(spiedOnMessage, TokenContract.tokenID)
                addCommand(spycmd)
            }
}

@InitiatedBy(UserWithTokenRegisterFlow::class)
class UserWithTokenRegisterFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {
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


