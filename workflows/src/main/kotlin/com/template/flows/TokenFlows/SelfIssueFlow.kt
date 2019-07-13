package com.template.flows.TokenFlows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TokenContract
import com.template.flows.*

import com.template.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier

import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap


@InitiatingFlow
@StartableByRPC
class SelfIssueFlow(private val walletBalance: Long ,private val linearId: UniqueIdentifier) : FlowFunctions() {
    @Suspendable
    override fun call() : SignedTransaction {
        progressTracker.currentStep = GENERATING_TRANSACTION
//        val userRegister = SelfISSUE(outputState())
        progressTracker.currentStep = VERIFYING_TRANSACTION
        progressTracker.currentStep = SIGNING_TRANSACTION
//        val signedTransaction = verifyAndSign(transaction = userRegister)
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val tx = verifyAndSign(transaction(spy))
        val spySession = initiateFlow(spy)
        val sessions = emptyList<FlowSession>() // empty because the owner's signature is just needed
        val transactionSignedByParties = collectSignature(transaction = tx, sessions = sessions)

        spySession.send(true)

        progressTracker.currentStep = FINALISING_TRANSACTION
        return recordTransaction(transaction = transactionSignedByParties, sessions = listOf(spySession))
    }

    private fun outputState(): TokenState {
        val input = inputStateRef(linearId).state.data
//        return TokenState(amount = input.amount.plus(amount),borrower = ourIdentity,lender = ourIdentity,linearId = linearId)
        return TokenState(amountIssued = 0,amountPaid = 0,borrower = ourIdentity,lender = ourIdentity,iss = input.iss,walletBalance = input.walletBalance.plus(walletBalance),linearId = linearId)

    }

//    private fun SelfISSUE(state: TokenState): TransactionBuilder {
//        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//        val cmd = Command(TokenContract.Commands.SelfIssue(),ourIdentity.owningKey)
//        val txBuilder = TransactionBuilder(notary)
//                .addInputState(inputStateRef())
//                .addOutputState(state, TokenContract.tokenID)
//                .addCommand(cmd)
//        return txBuilder
//    }
    /****
     *SPY*
     ****/
    private fun transaction(spy: Party) =
            TransactionBuilder(notary= inputStateRef(linearId).state.notary).apply {
//                val counterRef = stringToParty(counterParty)
                val spycmd = Command(TokenContract.Commands.Send(), listOf(ourIdentity.owningKey))
                val spiedOnMessage = outputState().copy(participants = outputState().participants + spy - outputState().lender)
                addInputState(inputStateRef(linearId))
                addOutputState(spiedOnMessage, TokenContract.tokenID)
                addCommand(spycmd)
            }
    /***/

}

@InitiatedBy(SelfIssueFlow::class)
class SelfIssueFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

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
