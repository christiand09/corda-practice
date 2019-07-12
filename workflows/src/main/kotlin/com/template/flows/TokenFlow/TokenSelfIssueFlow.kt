package com.template.flows.TokenFlow

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TokenContract
import com.template.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByRPC
class TokenSelfIssueFlow (val amount: Long, val linearId: String): TransactionFlows()
{
    @Suspendable
    override fun call(): SignedTransaction {
        val admin = serviceHub.identityService.partiesFromName("Admin",false).first()
        val command = Command(TokenContract.Commands.Token(), listOf(ourIdentity.owningKey))
        val notary = inputStateAndRefTokenState(linearId).state.notary
        val tx = verifyAndSign(transactionwithinput(admin,selfissue(),command,linearId,notary))
        val stx = subFlow(CollectSignaturesInitiatingFlow(tx, emptyList()))
        val session = listOf(admin).map { initiateFlow(it)}
        return subFlow(FinalityFlow(stx,session))
    }
    private fun selfissue(): TokenState
    {
        val input = inputStateAndRefTokenState(linearId).state.data
        return TokenState(input.details,ourIdentity, ourIdentity,input.requeststatus,
                input.total(amount).walletbalance, input.amountborrowed,input.amountpaid,
                linearId = stringToUniqueIdentifier(linearId))
    }
    @InitiatedBy(TokenSelfIssueFlow::class)
    class TokenSelfIssueFlowResponder(private val session: FlowSession) : FlowLogic<SignedTransaction>()
    {
        @Suspendable
        override fun call(): SignedTransaction {
            return subFlow(ReceiveFinalityFlow(otherSideSession = session))
        }
    }
}