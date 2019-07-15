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
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC

class SettleFlow (private val amount: Long,
                  private val counterParty: String,
                  private val linearId: UniqueIdentifier) : WalletFunction() {

    @Suspendable
    override fun call() : SignedTransaction {
        if (inputStateRef(linearId).state.data.issueStatus == "Partially Paid" || inputStateRef(linearId).state.data.issueStatus == "Amount Accepted") {
            val admin = stringToParty("PartyC")
            val tx = verifyAndSign(settle(admin))
            val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                    ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
            val session = initiateFlow(counterRef)
            val spySession = initiateFlow(admin)
            session.send(true)
            spySession.send(false)
            val transactionSignedByAllParties = collectSignature(tx, listOf(session))
            if (counterRef != inputStateRef(linearId).state.data.lender)
                throw FlowException("The borrower should settle the issue.")
            return recordTransaction(transactionSignedByAllParties, listOf(session, spySession))
        } else (inputStateRef(linearId).state.data.issueStatus == "Fully Paid")
            val admin = stringToParty("PartyC")
            val tx = verifyAndSign(settle(admin))
            val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                    ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
            val session = initiateFlow(counterRef)
            val spySession = initiateFlow(admin)
            session.send(false)
            spySession.send(true)
            val transactionSignedByAllParties = collectSignature(tx, listOf(session))
            if (counterRef != inputStateRef(linearId).state.data.lender)
                throw FlowException("The borrower should settle the issue.")
            return recordTransaction(transactionSignedByAllParties, listOf(session, spySession))
    }

    private fun outputState() : WalletState {
        val admin = stringToParty("PartyC")
        val counterRef = stringToParty(counterParty)
        val input = inputStateRef(linearId).state.data
        if (input.walletBalance < (input.amountBorrowed - input.amountPaid)) {
            throw FlowException("You only have ${input.walletBalance} but you need ${input.amountBorrowed} to settle.")
        }
        if (amount > input.amountBorrowed) {
            throw FlowException("The amount is not be greater than the amount borrowed.")
        }
        if (input.amountPaid.plus(amount) > input.amountBorrowed) {
            throw FlowException("You only owed ${input.amountBorrowed} but you pay a total of ${input.amountPaid.plus(amount)}.")
        } else if (input.amountPaid.plus(amount) < input.amountBorrowed) {
            return WalletState(name = input.name,
                    walletBalance = input.walletBalance.minus(amount),
                    amountBorrowed = input.amountBorrowed,
                    issueStatus = "Partially Paid",
                    amountPaid = input.amountPaid.plus(amount),
                    lender = counterRef,
                    borrower = ourIdentity,
                    linearId = linearId,
                    admin = admin)
        }
            return WalletState(name = input.name,
                    walletBalance = input.walletBalance.minus(amount),
                    amountBorrowed = 0,
                    issueStatus = "Fully Paid",
                    amountPaid = input.amountPaid.plus(amount),
                    lender = counterRef,
                    borrower = ourIdentity,
                    linearId = linearId,
                    admin = admin)
    }

    private fun settle(spy: Party) = TransactionBuilder(notary= serviceHub.networkMapCache.notaryIdentities.first()).apply {
        val counterRef = stringToParty(counterParty)
        val spycmd = Command(WalletContract.Commands.Settle(), listOf(ourIdentity.owningKey, counterRef.owningKey))
        if (outputState().issueStatus == "Partially Paid") {
            val spiedOnMessage = outputState().copy(participants = outputState().participants + spy)
            addInputState(inputStateRef(linearId))
            addOutputState(spiedOnMessage, WalletContract.WALLET_CONTRACT_ID)
            addCommand(spycmd)
        } else if (outputState().issueStatus == "Fully Paid") {
            val spiedOnMessage = outputState().copy(participants = outputState().participants + spy - outputState().borrower - outputState().lender)
            addInputState(inputStateRef(linearId))
            addOutputState(spiedOnMessage, WalletContract.WALLET_CONTRACT_ID)
            addCommand(spycmd)
        }
    }
}

@InitiatedBy(SettleFlow::class)
class SettleFlowResponder(private val session: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // receive the flag
        val needsToSignTransaction = session.receive<Boolean>().unwrap { it }
        // only sign if instructed to do so
        if (needsToSignTransaction) {
            subFlow(object : SignTransactionFlow(session) {
                override fun checkTransaction(stx: SignedTransaction) { }
            })
        }
        // always save the transaction
        return subFlow(ReceiveFinalityFlow(otherSideSession = session))
    }
}