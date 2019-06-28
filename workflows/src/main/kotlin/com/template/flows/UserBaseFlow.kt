package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.google.common.collect.ImmutableList
import com.template.flows.progressTracker.*
import com.template.states.UserState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

abstract class UserBaseFlow : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker(INITIALIZING, BUILDING, SIGNING, COLLECTING, FINALIZING)

    val firstNotary
        get() = serviceHub.networkMapCache.notaryIdentities.firstOrNull()
                ?: throw FlowException("No available notary.")

    fun getUserByLinearId(linearId: UniqueIdentifier): StateAndRef<UserState> {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                null,
                ImmutableList.of(linearId),
                Vault.StateStatus.UNCONSUMED, null)

        return serviceHub.vaultService.queryBy<UserState>(queryCriteria).states.singleOrNull()
                ?: throw FlowException("User with id $linearId not found.")
    }

     fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
         progressTracker.currentStep = SIGNING
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    @Suspendable
    fun collectSignature(
            transaction: SignedTransaction,
            sessions: List<FlowSession>
    ): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, sessions))

    @Suspendable
     fun recordTransaction(transaction: SignedTransaction, sessions: List<FlowSession>) : SignedTransaction {
        progressTracker.currentStep = FINALIZING
        return subFlow(FinalityFlow(transaction, sessions))
    }

}

internal class SignTxFlowNoChecking(otherFlow: FlowSession) : SignTransactionFlow(otherFlow) {
    override fun checkTransaction(stx: SignedTransaction) {
        // TODO: Add checking here.
    }
}