package com.template.flows.testflows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TestContract
import com.template.flows.testfunctions.TestFunctions
import com.template.states.TestState
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
import java.time.Duration
import java.time.Instant

@InitiatingFlow
@StartableByRPC
class TestVerifyFlow (private val status: Boolean,
                     private val counterParty: Party,
                     private val linearId: UniqueIdentifier): TestFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction {
        val issuance = issue()
        val sessions = initiateFlow(counterParty)
        val signedTransaction = verifyAndSign(issuance)
        val transactionSignedByParties = collectSignature(transaction = signedTransaction, sessions = listOf(sessions))
        return recordTransactionWithOtherParty(transaction = transactionSignedByParties, sessions = listOf(sessions))
    }

    private fun outState(): TestState
    {
        return TestState(
                status = status,
                party = ourIdentity,
                counter = counterParty,
                linearId = linearId
        )
    }

    private fun issue(): TransactionBuilder =
            TransactionBuilder(notary = inputStateRef(linearId).state.notary).apply {
                val registerTime = getTime(linearId)
                val timeWindow = TimeWindow.fromStartAndDuration(registerTime, Duration.ofSeconds(60))
                addInputState(inputStateRef(linearId))
                addOutputState(outState(), TestContract.TEST_ID)
                addCommand(TestContract.Commands.Issue(), listOf(ourIdentity.owningKey, counterParty.owningKey))
                setTimeWindow(timeWindow)
            }
}

@InitiatedBy(TestVerifyFlow::class)
class TestVerifyFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction {
        subFlow(object : SignTransactionFlow(flowSession)
        {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an issue transaction" using (output is TestState)
            }
        })
        return subFlow(net.corda.core.flows.ReceiveFinalityFlow(otherSideSession = flowSession))
    }
}

