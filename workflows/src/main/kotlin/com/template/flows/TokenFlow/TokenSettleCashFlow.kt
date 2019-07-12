package com.template.flows.TokenFlow

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TokenContract
import com.template.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByRPC
class TokenSettleCashFlow (val linearId: String, val lender: String, val amount: Long): TransactionFlows()
{
    @Suspendable
    override fun call(): SignedTransaction {
        val admin = serviceHub.identityService.partiesFromName("Admin",false).first()
        val settle: SignedTransaction
        val lend = stringToParty(lender)
        val command = Command(TokenContract.Commands.Token(), listOf(ourIdentity.owningKey,lend.owningKey))
        val notary = inputStateAndRefTokenState(linearId).state.notary
        if(inputsettle().requeststatus == "partially paid")
        {
            settle = verifyAndSign(transactionwithinput(admin,inputsettle(),command,linearId,notary))
        }
        else
        {
            settle = verifyAndSign(fullypaidtransaction(admin,inputsettle(),command,linearId,notary))
        }
        val stx = subFlow(CollectSignaturesInitiatingFlow(settle, listOf(lend)))
        val session = listOf(admin,lend).map { initiateFlow(it) }
        return subFlow(FinalityFlow(stx,session))
    }
    private fun inputsettle(): TokenState
    {
        val lend = stringToParty(lender)
        val input = inputStateAndRefTokenState(linearId).state.data
        if(amount > input.amountborrowed) throw FlowException("You only owed ${input.amountborrowed} but gives $amount which is more than the amount owed")
        val paid = input.paidamount(amount).amountpaid
        if(paid > input.amountborrowed) throw FlowException("You only owed ${input.amountborrowed - input.amountpaid} but gives $amount which is more than the amount owed")
        if(input.walletbalance < amount) throw FlowException("You don't have enough balance in your wallet to pay for the amount you owed")
        if( paid  < input.amountborrowed)
        {
            return TokenState(input.details,lend,
                    input.borrower,"partially paid", input.settle(amount).walletbalance,
                    input.amountborrowed,input.paidamount(amount).amountpaid, linearId = stringToUniqueIdentifier(linearId))
        }
        else
        {
            return TokenState(input.details, lend,
                    input.borrower, "fully paid", input.settle(amount).walletbalance,
                    input.amountborrowed, paid, linearId = stringToUniqueIdentifier(linearId))
        }
    }
    @InitiatedBy(TokenSettleCashFlow::class)
    class SettleFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
        }
    }
}