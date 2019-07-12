package com.template.flows.TokenFlow

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TokenContract
import com.template.states.Details
import com.template.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByRPC
class TokenRegisterFlow (val user: Details): TransactionFlows()
{
    @Suspendable
    override fun call(): SignedTransaction {
        val admin = serviceHub.identityService.partiesFromName("Admin",false).first()
        val command = Command(TokenContract.Commands.Token(), listOf(ourIdentity.owningKey))
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val tx = verifyAndSign(transactionwithoutinput(admin,tokenregister(),command,notary))
        val stx = subFlow(CollectSignaturesInitiatingFlow(tx, emptyList()))
        val sessions = listOf(admin).map { initiateFlow(it) }
        return subFlow(FinalityFlow(stx,sessions))
    }
    private fun tokenregister(): TokenState
    {
        return TokenState(user,ourIdentity, ourIdentity,"none", 0,0, 0)
    }
    @InitiatedBy(TokenRegisterFlow::class)
    class TokenRegisterFlowResponder(private val session: FlowSession) :FlowLogic<SignedTransaction>()
    {
        @Suspendable
        override fun call(): SignedTransaction {
            return subFlow(ReceiveFinalityFlow(otherSideSession = session))
        }
    }
}
