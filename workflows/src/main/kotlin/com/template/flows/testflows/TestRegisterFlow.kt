package com.template.flows.testflows

import com.template.flows.testfunctions.TestFunctions
import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TestContract
import com.template.states.TestState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
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
        // API
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

@InitiatedBy(TestRegisterFlow::class)
class TestRegisterFlowResponder(private val flowSession: FlowSession): FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        subFlow(object : SignTransactionFlow(flowSession)
        {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a transfer transaction" using (output is TestState)
            }
        })
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
    }
}