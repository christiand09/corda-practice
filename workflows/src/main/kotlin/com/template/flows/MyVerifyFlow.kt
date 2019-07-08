package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.MyContract
import com.template.contracts.MyContract.Companion.ID
import com.template.states.MyState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class MyVerifyFlow(private val counterParty: String, private val linearId:UniqueIdentifier) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val unVerified = userVerified()
        val signedTransaction = verifyAndSign(unVerified)
        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
        val sessions=initiateFlow(counterRef)
        val transactionSignedByAllParties: SignedTransaction=collectSignature(signedTransaction, listOf(sessions) )
        return recordTransaction(transactionSignedByAllParties, listOf(sessions) )
    }
    private fun inputStateRef(): StateAndRef<MyState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<MyState>(criteria).states.single()
    }
    private fun outputState():MyState{
        val input = inputStateRef().state.data
        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
        return MyState(
                input.registered,
                ourIdentity,
                counterRef,
                true,
                input.linearId)

    }
    private fun userVerified(): TransactionBuilder{
        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
        val notary = inputStateRef().state.notary
        val cmd = Command ( MyContract.Commands.Verify(), listOf(ourIdentity.owningKey, counterRef.owningKey))
        return TransactionBuilder(notary)
                .addInputState(inputStateRef())
                .addOutputState(outputState(), ID)
                .addCommand(cmd)
    }
    private fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction{
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



@InitiatedBy(MyVerifyFlow::class)
class MyVerifyFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction" using (output is MyState)
            }
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}