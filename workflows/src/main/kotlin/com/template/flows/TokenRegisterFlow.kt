package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TokenContract
import com.template.states.Details
import com.template.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class TokenRegisterFlow (val user: Details): FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction {
        val register:TransactionBuilder = transaction(tokenregister())
        val signedTransaction:SignedTransaction = verifyandsign(register)
        val session:List<FlowSession> = emptyList()
        return recordToken(signedTransaction,session)
    }
    private fun transaction(state: TokenState): TransactionBuilder
    {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val command = Command(TokenContract.Commands.Token(), listOf(ourIdentity.owningKey))
        val builder = TransactionBuilder(notary = notary)
                .addOutputState(state = state,contract = TokenContract.TOKEN_ID)
                .addCommand(command)
        return builder
    }
    private fun tokenregister(): TokenState
    {
        return TokenState(user,ourIdentity,ourIdentity,"none",0,0,0)
    }
    private fun verifyandsign(transaction: TransactionBuilder):SignedTransaction
    {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }
    @Suspendable
    private fun recordToken(transaction: SignedTransaction,session: List<FlowSession>): SignedTransaction
    = subFlow(FinalityFlow(transaction,session))


}
