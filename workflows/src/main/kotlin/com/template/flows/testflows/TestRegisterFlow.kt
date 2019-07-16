package com.template.flows.testflows

import com.template.flows.testfunctions.TestFunctions
import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TestContract
import com.template.states.TestState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.sql.Time
import java.time.Instant
import java.time.LocalTime

@InitiatingFlow
@StartableByRPC
class TestRegisterFlow : TestFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val registration = register()
        val signedTransaction = verifyAndSign(transaction = registration)
        val sessions = emptyList<FlowSession>()
        val transactionSignedByParties = collectSignature(transaction = signedTransaction, sessions = sessions)
        return recordTransactionWithOtherParty(transaction = transactionSignedByParties, sessions = sessions)
    }

    private fun outState(): TestState
    {
        return TestState(
                status = false,
                party = ourIdentity,
                counter = ourIdentity
        )
    }

    private fun register(): TransactionBuilder =
                TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first()).apply {
                    addOutputState(outState(), TestContract.TEST_ID)
                    addCommand(Command(TestContract.Commands.Register(), ourIdentity.owningKey))
                }
}