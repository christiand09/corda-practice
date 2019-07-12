package com.template.flows.UserFlow

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.UserDetails
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class UserRegisterFlow(private val name: UserDetails):FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call():SignedTransaction
    {
        val transaction: TransactionBuilder = transaction(registerstate())
        val signedTransaction:SignedTransaction = verifyandSign(transaction)
        val session:List<FlowSession> = emptyList()
        val transactionsSignedBothbyParties: SignedTransaction = signedTransaction
        return recordRegister(transaction = transactionsSignedBothbyParties,session = session)
    }
    private fun registerstate(): UserState
    {
        return UserState(name,ourIdentity,ourIdentity)
    }
    private fun transaction(state: UserState): TransactionBuilder
    {
        val notary : Party = serviceHub.networkMapCache.notaryIdentities.first()
        val issueCommand=
                Command(UserContract.Commands.Register(), ourIdentity.owningKey)
        val builder = TransactionBuilder(notary = notary)
                .addCommand(issueCommand)
                .addOutputState(state = state, contract = UserContract.ID_Contracts)
        return builder
    }
    private fun verifyandSign(transaction: TransactionBuilder): SignedTransaction
    {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }
    @Suspendable
    private fun recordRegister(transaction: SignedTransaction,session: List<FlowSession>): SignedTransaction =
            subFlow(FinalityFlow(transaction,session))
}