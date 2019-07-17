package com.template.flows.timeStamp

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TimeStampContract
import com.template.states.TimeStampState
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Duration

@InitiatingFlow
@StartableByRPC

class TimeStampVerifyFlow (val counterParty: Party,
                           val linearId: UniqueIdentifier
):FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val session = initiateFlow(counterParty)
        val signedTransaction = verifyAndSign(transactionBuilder())
        val transactionSignedByAllParties = collectSignature(signedTransaction, listOf(session))
        return recordTransaction(transactionSignedByAllParties, listOf(session))
    }

    private fun inputStateRef(): StateAndRef<TimeStampState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId =  listOf(linearId))
        return serviceHub.vaultService.queryBy<TimeStampState>(criteria).states.single()
    }

    private fun outputState (): TimeStampState {
        return TimeStampState(condition = true, sender = ourIdentity, receiver = counterParty)
    }

    private fun transactionBuilder() : TransactionBuilder{
        val notary = inputStateRef().state.notary
        val cmd = Command (TimeStampContract.Commands.Verify(),
                listOf(ourIdentity.owningKey,counterParty.owningKey))

        val outputStateRef = StateRef(txhash = inputStateRef().ref.txhash, index = 0)
        val queryCriteria = QueryCriteria.VaultQueryCriteria(stateRefs = listOf(outputStateRef))
        val results = serviceHub.vaultService.queryBy<TimeStampState>(queryCriteria)
        val recordTime = results.statesMetadata.single().recordedTime

        val timeWindow = TimeWindow.fromStartAndDuration(recordTime, Duration.ofSeconds(60))

        return TransactionBuilder(notary)
                .addInputState(inputStateRef())
                .addOutputState(outputState(),TimeStampContract.ID)
                .addCommand(cmd)
                .setTimeWindow(timeWindow)
    }

    private fun verifyAndSign(transactionBuilder: TransactionBuilder):SignedTransaction{
        transactionBuilder.verify(serviceHub)
        return serviceHub.signInitialTransaction(transactionBuilder)
    }

    @Suspendable
    private fun collectSignature(
            transaction: SignedTransaction,
            session: List<FlowSession>
    ):SignedTransaction=subFlow(CollectSignaturesFlow(transaction,session))

    @Suspendable
    private fun recordTransaction (
            transaction: SignedTransaction,
            session: List<FlowSession>
    ):SignedTransaction = subFlow(FinalityFlow(transaction , session))

}

@InitiatedBy(TimeStampVerifyFlow::class)
class TimeStampVerifyFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction" using (output is TimeStampState)
            }
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}