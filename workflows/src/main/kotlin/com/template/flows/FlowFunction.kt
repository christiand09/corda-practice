package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

abstract class FlowFunction : FlowLogic<SignedTransaction>() {
    fun verifyAndSign(transactionBuilder: TransactionBuilder):SignedTransaction {
        transactionBuilder.verify(serviceHub)
        return serviceHub.signInitialTransaction(transactionBuilder)
    }
    @Suspendable
    fun collectSignature(
            transaction: SignedTransaction,
            sessions: List<FlowSession>
    ):SignedTransaction=subFlow(CollectSignaturesFlow(transaction,sessions))

    @Suspendable
    fun recordTransactionWithParty(
            transaction: SignedTransaction,
            sessions: List<FlowSession>
    ):SignedTransaction=subFlow(FinalityFlow(transaction,sessions))

    @Suspendable
    fun recordTransactionWithOutParty(
            transaction: SignedTransaction
    ):SignedTransaction=subFlow(FinalityFlow(transaction, emptyList()))
}