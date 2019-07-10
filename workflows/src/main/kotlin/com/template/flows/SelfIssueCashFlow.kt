package com.template.flows
//
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command

import net.corda.core.contracts.requireThat

import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import com.template.states.MyState
import com.template.contracts.MyContract
//import com.template.states.formSet
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria

@InitiatingFlow
@StartableByRPC
class SelfIssueCashFlow(private val amount: Int,  private val linearId: UniqueIdentifier = UniqueIdentifier()
): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val transaction: TransactionBuilder = transaction()
        val signedTransaction: SignedTransaction = verifyAndSign(transaction)
        val sessions = emptyList<FlowSession>()
        val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, sessions)
        return recordTransaction(transactionSignedByAllParties, sessions)
    }
    private fun inputStateRef(): StateAndRef<MyState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<MyState>(criteria).states.single()
    }
    private fun outputState(): MyState {
        val input = inputStateRef().state.data
        return MyState(
                input.formSet,
                ourIdentity,
                ourIdentity,
                wallet = input.wallet + amount,
                amountdebt = input.amountdebt,
                amountpaid = input.amountpaid,
                linearId = linearId,
                status = "Now issuing cash on $ourIdentity",
                approvals = input.approvals,
                participants = listOf(ourIdentity)
        )
    }
    private fun transaction(): TransactionBuilder {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//        val MyState = MyState(firstName,lastName,age, gender,address, ourIdentity, ourIdentity)
        val issueCommand = Command(MyContract.Commands.Issue(),ourIdentity.owningKey)
        val builder = TransactionBuilder(notary = notary )
        builder.addInputState(inputStateRef())
        builder.addOutputState(outputState(), MyContract.IOU_CONTRACT_ID)
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

/**
 * This is the flow which signs IOU issuances.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(SelfIssueCashFlow::class)
class SelfIssueCashFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

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

//@InitiatingFlow
//@StartableByRPC
//class SelfIssueCashFlow(private val amount: Int):FlowLogic<SignedTransaction>() {
//
//    @Suspendable
//    override fun call(): SignedTransaction {
////        progressTracker.currentStep = GENERATING_TRANSACTION
//        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//        val tokenState = MyState(amount, ourIdentity, listOf(ourIdentity))
//        val sessions = emptyList<FlowSession>()
//        val transactionBuilder = TransactionBuilder(notary)
//                .addOutputState(tokenState, MyContract.IOU_CONTRACT_ID)
//                .addCommand(MyContract.Commands.Issue(), listOf(ourIdentity.owningKey))
//
//        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
//        transactionBuilder.verify(serviceHub)
//        return subFlow(FinalityFlow(signedTransaction, sessions))
//    }
//}