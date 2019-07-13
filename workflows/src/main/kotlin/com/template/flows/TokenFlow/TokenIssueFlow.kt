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
class TokenIssueFlow (val amount:Long, val lender: String, val linearId: UniqueIdentifier): TransactionFlows() {

    @Suspendable
    override fun call(): SignedTransaction {
        val admin = serviceHub.identityService.partiesFromName("Admin", false).first()
        val lend = stringToParty(lender)
        val notary = inputStateAndRefTokenState(linearId).state.notary
        val command = Command(TokenContract.Commands.Token(), listOf(lend.owningKey,ourIdentity.owningKey))
        val tx = verifyAndSign(transactionwithinput(admin, issuerequeststate(),command,linearId,notary))
        val stx = subFlow(CollectSignaturesInitiatingFlow(tx, listOf(lend)))
        val session = listOf(admin, lend).map { initiateFlow(it) }
        return subFlow(FinalityFlow(stx, session))
    }
    private fun issuerequeststate(): TokenState {
        val input = inputStateAndRefTokenState(linearId).state.data
        val lend = stringToParty(lender)
        return (TokenState(input.details, lend, ourIdentity, "pending",
                input.walletbalance, amount,
                input.amountpaid, linearId = linearId))
    }
    @InitiatedBy(TokenIssueFlow::class)
    class FlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {

            return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
        }
    }
}