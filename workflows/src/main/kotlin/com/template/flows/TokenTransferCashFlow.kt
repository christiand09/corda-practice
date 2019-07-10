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
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class TokenTransferCashFlow (val linearId: UniqueIdentifier, val borrower: Party): FlowLogic<SignedTransaction>()
{
    override val progressTracker: ProgressTracker = tracker()
    companion object
    {
        object CREATING : ProgressTracker.Step("Creating registration!")
        object SIGNING : ProgressTracker.Step("Signing registration!")
        object VERIFYING : ProgressTracker.Step("Verifying registration!")
        object FINALISING : ProgressTracker.Step("Finalize registration!")
        {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(CREATING, SIGNING, VERIFYING, FINALISING)
    }
    @Suspendable
    override fun call(): SignedTransaction {

        progressTracker.currentStep = CREATING
        val issuecash = transaction(inputstate())
        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyandsign(issuecash)
        val session = initiateFlow(borrower)
        val transactionsignedbyboth = collectIssuesignature(signedTransaction, listOf(session))
        progressTracker.currentStep = FINALISING
        return recordIssue(transactionsignedbyboth, listOf(session))
    }
    private fun stateref(): StateAndRef<TokenState>
    {
        val stateref = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<TokenState>(stateref).states.single()
    }
    private fun inputstate(): TokenState
    {
        val input = stateref().state.data
        return TokenState(input.details,input.lender,input.borrower,"approved",input.total(input.amountborrowed).walletbalance,input.amountborrowed,input.amountpaid,linearId = linearId)
    }
    private fun transaction(state: TokenState): TransactionBuilder
    {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val command = Command(TokenContract.Commands.Token(),listOf(ourIdentity.owningKey,borrower.owningKey))
        val builder = TransactionBuilder(notary = notary)
                .addCommand(command)
                .addOutputState(state = state,contract = TokenContract.TOKEN_ID)
                .addInputState(stateref())
        return builder
    }
    private fun verifyandsign(transaction: TransactionBuilder): SignedTransaction
    {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }
    @Suspendable
    private fun collectIssuesignature(transaction: SignedTransaction,session: List<FlowSession>): SignedTransaction
    = subFlow(CollectSignaturesFlow(transaction,session))
    @Suspendable
    private fun recordIssue(transaction: SignedTransaction,session: List<FlowSession>):SignedTransaction
     = subFlow(FinalityFlow(transaction,session))

    @InitiatedBy(TokenTransferCashFlow::class)
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