package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TokenContract
import com.template.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class TokenSelfIssueFlow (val amount: Long, val linearId: UniqueIdentifier): FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction {

        val transaction:TransactionBuilder = transaction(selfissue())
        val signedTransaction:SignedTransaction = verifyandsign(transaction)
        val session: List<FlowSession> = emptyList()
        return recordIssue(transaction = signedTransaction,session = session)
    }
    private fun transaction(state: TokenState): TransactionBuilder
    {
        val notary = stateref().state.notary
        val command = Command(TokenContract.Commands.Token(), listOf(ourIdentity.owningKey))
        val builder = TransactionBuilder(notary = notary)
                .addInputState(stateref())
                .addOutputState(state = state, contract = TokenContract.TOKEN_ID)
                .addCommand(command)
        return builder
    }

    private fun selfissue(): TokenState
    {
        val input = stateref().state.data
        return TokenState(input.details,false,ourIdentity,ourIdentity, input.total(amount).walletbalance,linearId = linearId)
    }
    private fun stateref(): StateAndRef<TokenState>
    {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<TokenState>(criteria).states.single()
    }
    private fun verifyandsign(transaction:TransactionBuilder): SignedTransaction
    {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }
    @Suspendable
    private fun recordIssue(transaction: SignedTransaction,session: List<FlowSession>): SignedTransaction
    = subFlow(FinalityFlow(transaction,session))
}