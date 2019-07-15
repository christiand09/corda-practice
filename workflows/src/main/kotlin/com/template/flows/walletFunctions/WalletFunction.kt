package com.template.flows.walletFunctions

import co.paralleluniverse.fibers.Suspendable
import com.template.states.WalletState
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

abstract class WalletFunction : FlowLogic<SignedTransaction>() {
    fun verifyAndSign (transaction: TransactionBuilder) : SignedTransaction {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    @Suspendable
    fun collectSignature (transaction: SignedTransaction, session: List<FlowSession>
    ) : SignedTransaction = subFlow(CollectSignaturesFlow(transaction, session))

    @Suspendable
    fun recordTransaction (transaction: SignedTransaction, session: List<FlowSession>
    ) : SignedTransaction = subFlow(FinalityFlow(transaction,session))

    fun inputStateRef(linearId: UniqueIdentifier) : StateAndRef<WalletState> {
        val ref = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<WalletState>(ref).states.single()
    }

    fun stringToParty(name: String): Party {
        return serviceHub.identityService.partiesFromName(name, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for $name")
    }
}

