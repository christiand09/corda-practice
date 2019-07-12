package com.template.flows.registerfunctions

import co.paralleluniverse.fibers.Suspendable
import com.template.states.RegisterState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

abstract class CashFunctions : FlowLogic<SignedTransaction>()
{
    fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {

        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    @Suspendable
    fun collectSignature(
            transaction: SignedTransaction,
            sessions: List<FlowSession>
    ): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, sessions))

    @Suspendable
    fun recordTransactionWithOtherParty(transaction: SignedTransaction, sessions: List<FlowSession>) : SignedTransaction {
        return subFlow(FinalityFlow(transaction, sessions))
    }

    fun stringToParty(name: String): Party {
        return serviceHub.identityService.partiesFromName(name, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for $name")
    }

    fun inputStateRef(id: UniqueIdentifier): StateAndRef<RegisterState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(id))
        return serviceHub.vaultService.queryBy<RegisterState>(criteria = criteria).states.single()
    }
}
