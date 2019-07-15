package com.template.flows.walletFlows


import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.WalletContract
import com.template.flows.walletFunctions.WalletFunction
import com.template.states.WalletState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class SelfIssueFlow (private val amount: Long ,
                     private val linearId: UniqueIdentifier) : WalletFunction() {

    @Suspendable
    override fun call (): SignedTransaction {
        val admin = stringToParty("PartyC")
        val tx = verifyAndSign(selfIssue(admin))
        val sessions = emptyList<FlowSession>()
        val spySession = initiateFlow(admin)
        val transactionSignedByAllParties = collectSignature(tx, sessions)
        return recordTransaction(transactionSignedByAllParties, listOf(spySession))
    }

    private fun selfIssueState() : WalletState {
        val admin = stringToParty("PartyC")
        val input = inputStateRef(linearId).state.data
        return WalletState(input.name,
                walletBalance = input.walletBalance.plus(amount),
                amountBorrowed = input.amountBorrowed,
                issueStatus = "",
                amountPaid = input.amountPaid,
                lender = ourIdentity,
                borrower = ourIdentity,
                linearId = linearId,
                admin = admin)
    }

    private fun selfIssue(spy: Party) = TransactionBuilder(notary= serviceHub.networkMapCache.notaryIdentities.first()).apply {
        val spycmd = Command(WalletContract.Commands.SelfIssue(), ourIdentity.owningKey)
        val spiedOnMessage = selfIssueState().copy(participants = selfIssueState().participants + spy)
        addInputState(inputStateRef(linearId))
        addOutputState(spiedOnMessage, WalletContract.WALLET_CONTRACT_ID)
        addCommand(spycmd)
    }
}

@InitiatedBy(SelfIssueFlow::class)
class SelfIssueFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
    }
}


