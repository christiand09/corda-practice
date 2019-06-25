package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.states.TheContract
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class TheFlow(val state: TheContract.TheState) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val transaction: TransactionBuilder = transaction()
        val signedTransaction: SignedTransaction = verifyAndSign(transaction)
        val sessions: List<FlowSession> = (state.participants - ourIdentity).map { initiateFlow(it) }.toSet().toList()
        val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, sessions)
        return recordTransaction(transactionSignedByAllParties, sessions)
    }

    private fun transaction(): TransactionBuilder {
        val notary: Party = serviceHub.networkMapCache.notaryIdentities.first()
        val issueCommand: Command<com.template.contracts.TheContract.Commands.Register> =
                Command(com.template.contracts.TheContract.Commands.Register(), state.participants.map { it.owningKey })
        val builder = TransactionBuilder(notary = notary)
        builder.addOutputState(state, com.template.contracts.TheContract.The_Contract_ID)
        builder.addCommand(issueCommand)
        return builder
    }

    private fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    @Suspendable
    private fun collectSignature(
            transaction: SignedTransaction,
            sessions: List<FlowSession>
    ): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, sessions))

    @Suspendable
    private fun recordTransaction(transaction: SignedTransaction, sessions: List<FlowSession>): SignedTransaction =
            subFlow(FinalityFlow(transaction, sessions))
}