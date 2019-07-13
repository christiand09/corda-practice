package com.template.flows.TokenFlow

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TokenContract
import com.template.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByRPC
class TokenTransferCashFlow (val linearId: UniqueIdentifier, val borrower: String): TransactionFlows()
{
    @Suspendable
    override fun call(): SignedTransaction {
        val admin = serviceHub.identityService.partiesFromName("Admin",false).first()
        val borrow = stringToParty(borrower)
        val command = Command(TokenContract.Commands.Token(), listOf(ourIdentity.owningKey,borrow.owningKey))
        val notary = inputStateAndRefTokenState(linearId).state.notary
        val issuecash = verifyAndSign(transactionwithinput(admin,inputstate(),command,linearId,notary))
        val stx = subFlow(CollectSignaturesInitiatingFlow(issuecash, listOf(borrow)))
        val session = listOf(admin,borrow).map { initiateFlow(it) }
        return subFlow(FinalityFlow(stx,session))
    }
    private fun inputstate(): TokenState
    {
        val input = inputStateAndRefTokenState(linearId).state.data
        return TokenState(input.details,input.lender, input.borrower,"approved"
                ,input.total(input.amountborrowed).walletbalance, input.amountborrowed,input.amountpaid,
                linearId = linearId)
    }
    @InitiatedBy(TokenTransferCashFlow::class)
    class TransferFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
        }
    }
}