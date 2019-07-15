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

class TransferFlow (private val amount: Long,
                    private val counterParty: String,
                    private val linearId: UniqueIdentifier) : WalletFunction() {

    @Suspendable
    override fun call() : SignedTransaction {
        val admin = stringToParty("PartyC")
        val tx = verifyAndSign(transfer(admin))
        val counterRef = stringToParty(counterParty)
        val session = initiateFlow(counterRef)
        val spySession = initiateFlow(admin)
        session.send(true)
        spySession.send(false)
        val transactionSignedByAllParties = collectSignature(tx, listOf(session))
        return recordTransaction(transactionSignedByAllParties, listOf(session,spySession))
    }

    private fun outputState() : WalletState {
        val input = inputStateRef(linearId).state.data
        val admin = stringToParty("PartyC")
        val counterRef = stringToParty(counterParty)

        return WalletState(name = input.name,
                walletBalance = input.walletBalance.plus(amount),
                amountBorrowed = input.amountBorrowed,
                issueStatus = "Amount Accepted",
                amountPaid = 0,
                lender = ourIdentity,
                borrower = counterRef,
                linearId = linearId,
                admin = admin)
    }

    private fun transfer(spy: Party) = TransactionBuilder(notary= serviceHub.networkMapCache.notaryIdentities.first()).apply {
        val counterRef = stringToParty(counterParty)
        val spycmd = Command(WalletContract.Commands.Transfer(), listOf(ourIdentity.owningKey, counterRef.owningKey))
        val spiedOnMessage = outputState().copy(participants = outputState().participants + spy)
        addInputState(inputStateRef(linearId))
        addOutputState(spiedOnMessage, WalletContract.WALLET_CONTRACT_ID)
        addCommand(spycmd)
    }
}

@InitiatedBy(TransferFlow::class)
class TransferFlowResponder(private val session: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val needsToSignTransaction = session.receive<Boolean>().unwrap { it }
        if (needsToSignTransaction) {
            subFlow(object : SignTransactionFlow(session) {
                override fun checkTransaction(stx: SignedTransaction) { }
            })
        }

        return subFlow(ReceiveFinalityFlow(otherSideSession = session))
    }
}