package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TokenContract
import com.template.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class TokenSettleCashFlow (val linearId: UniqueIdentifier, val lender: Party, val amount: Long):FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction {

        val settle = transaction(inputsettle())
        val signtransaction = verifyandsign(settle)
        val session = initiateFlow(lender)
        val transactionssignedbyboth = collectsignature(signtransaction, listOf(session))
        return recordSettle(transactionssignedbyboth, listOf(session))
    }
    private fun inputstateref(): StateAndRef<TokenState>
    {
        val stateref = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<TokenState>(stateref).states.first()
    }
    private fun transaction(state:TokenState): TransactionBuilder
    {
        val notary = inputstateref().state.notary
        val command = Command(TokenContract.Commands.Token(), listOf(ourIdentity.owningKey,lender.owningKey))
        val builder = TransactionBuilder(notary = notary)
                .addInputState(inputstateref())
                .addOutputState(state = state,contract = TokenContract.TOKEN_ID)
                .addCommand(command)
        return builder
    }
    private fun inputsettle(): TokenState
    {
        val input = inputstateref().state.data
        if(amount > input.amountborrowed) throw FlowException("You only owed ${input.amountborrowed} but gives $amount which is more than the amount owed")
        if(input.paidamount(amount).amountpaid > input.amountborrowed) throw FlowException("You only owed ${input.amountborrowed - input.amountpaid} but gives $amount which is more than the amount owed")
        if(input.amountpaid  < input.amountborrowed)
        {
            return TokenState(input.details,lender,
                    input.borrower,"partially paid",
                    input.settle(amount).walletbalance,
                    input.amountborrowed,input.paidamount(amount).amountpaid,
                    linearId = linearId)
        }
        else {
            return TokenState(input.details, lender,
                    input.borrower, "fully paid",
                    input.settle(amount).walletbalance,
                    input.amountborrowed, input.paidamount(amount).amountpaid,
                    linearId = linearId)
        }
    }
    private fun verifyandsign(transaction: TransactionBuilder): SignedTransaction
    {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }
    @Suspendable
    private fun collectsignature(transaction: SignedTransaction,session: List<FlowSession>): SignedTransaction
     = subFlow(CollectSignaturesFlow(transaction,session))
    @Suspendable
    private fun recordSettle(transaction: SignedTransaction,session: List<FlowSession>):SignedTransaction
    = subFlow(FinalityFlow(transaction,session))

    @InitiatedBy(TokenSettleCashFlow::class)
    class IOUIssueFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an IOU transaction" using (output is TokenState)
                }
            }

            val txWeJustSignedId = subFlow(signedTransactionFlow)

            return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
        }
    }
}