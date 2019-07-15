package com.template.flows.walletFlows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.WalletContract
import com.template.flows.walletFunctions.WalletFunction
import com.template.states.WalletState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class WalletRegisterFlow (private val name: String) : WalletFunction() {

    @Suspendable
    override fun call() : SignedTransaction {
        val admin = stringToParty("PartyC")
        val tx = verifyAndSign(register(admin))
        val sessions = emptyList<FlowSession>()
        val spySession = initiateFlow(admin)
        val transactionSignedByAllParties = collectSignature(tx, sessions)
        return recordTransaction(transactionSignedByAllParties , listOf(spySession))
    }

    private fun outputState() : WalletState {
        val admin = stringToParty("PartyC")
        return WalletState(name,
                0,
                0,
                "",
                0,
                lender = ourIdentity,
                borrower = ourIdentity,
                admin = admin)
    }

    private fun register(spy: Party) = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first()).apply {
        val spycmd = Command(WalletContract.Commands.WalletRegister(), ourIdentity.owningKey)
        // the spy is added to the messages participants
        val spiedOnMessage = outputState().copy(participants = outputState().participants + spy)
        addOutputState(spiedOnMessage, WalletContract.WALLET_CONTRACT_ID)
        addCommand(spycmd)
    }
}

@InitiatedBy(WalletRegisterFlow::class)
class WalletRegisterFlowResponder(private val session: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        return subFlow(ReceiveFinalityFlow(otherSideSession = session))
    }
}