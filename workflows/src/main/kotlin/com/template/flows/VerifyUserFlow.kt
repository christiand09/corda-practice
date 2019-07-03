package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.MyContract
import com.template.states.MyState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder


@InitiatingFlow
@StartableByRPC
class VerifyUserFlow (
        private val receiver: Party,
        private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val transaction: TransactionBuilder = transaction()
        val signedTransaction: SignedTransaction = verifyAndSign(transaction)
        //val sessions = initiateFlow(receiver)
        val sessions = (outputState().participants - ourIdentity).map { initiateFlow(it) }.toList()
//        val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, listOf(sessions))
        val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, sessions)
        return recordTransaction(transactionSignedByAllParties, sessions)
    }

    private fun inputStateRef(): StateAndRef<MyState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<MyState>(criteria).states.single()
    }

    private fun outputState(): MyState{
        val input = inputStateRef().state.data
        return MyState(input.firstName,input.lastName,input.age,input.gender,input.address,ourIdentity, receiver, true,linearId = linearId)
    }

    private fun transaction(): TransactionBuilder {

//        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
//        val vault = serviceHub.vaultService.queryBy<MyState>(criteria).states.single()
//        val input = vault.state.data
//        val notary = vault.state.notary
 //       val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val notary = inputStateRef().state.notary
//        val outputState = MyState(input.firstName, input.lastName, input.age, input.gender, input.address, sender = ourIdentity,receiver = receiver, true,approvals = true,linearId = input.linearId)
//       val cmd = Command(MyContract.Commands.Issue(), listOf(receiver.owningKey, ourIdentity.owningKey))
        val cmd = Command(MyContract.Commands.Issue(), outputState().participants.map { it.owningKey })
        val builder = TransactionBuilder(notary = notary)
                .addOutputState(outputState(), MyContract.IOU_CONTRACT_ID)
                .addCommand(cmd)
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


@InitiatedBy(VerifyUserFlow::class)
class VerifyFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {
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

