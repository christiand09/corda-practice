package com.template.flows.UserwithTimeWindow

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.UserDetails
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class Register (private val name: UserDetails): Methods()
{
    @Suspendable
    override fun call(): SignedTransaction {

        val command = Command(UserContract.Commands.Register(),ourIdentity.owningKey)
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val transaction: TransactionBuilder = transactionwithoutinput(input(),command,notary)
        val signedTransaction:SignedTransaction = verifyAndSign(transaction)
        val transactionsSignedBothbyParties: SignedTransaction = signedTransaction
        return subFlow(FinalityFlow(transactionsSignedBothbyParties, emptyList()))
}
    private fun input(): UserState
    {
        return UserState(name,ourIdentity,ourIdentity)
    }
}