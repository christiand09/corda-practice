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
class TokenIssueFlow (val amount:Long, val lender: Party, val linearId: UniqueIdentifier): FlowLogic<SignedTransaction>()
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
        val request  = transaction()
        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyandsign(request)
        val session = initiateFlow(lender)
        val transactionsignedbybothparties = collectsignatures(signedTransaction, listOf(session))
        progressTracker.currentStep = FINALISING
        return recordIssue(transactionsignedbybothparties, listOf(session))
    }
    private fun transaction(): TransactionBuilder
    {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val command = Command(TokenContract.Commands.Token(), listOf(ourIdentity.owningKey,lender.owningKey))
        val builder = TransactionBuilder(notary = notary)
                .addOutputState(state = issuerequeststate(),contract = TokenContract.TOKEN_ID )
                .addInputState(inputstateref())
                .addCommand(command)
        return builder
    }
    private fun inputstateref(): StateAndRef<TokenState>
    {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<TokenState>(criteria).states.single()
    }
    private fun issuerequeststate():TokenState
    {
        val input = inputstateref().state.data
        return(TokenState(input.details,lender,ourIdentity,"pending",input.walletbalance,amount,input.amountpaid,linearId = linearId))
    }
    private fun verifyandsign(transaction:TransactionBuilder): SignedTransaction
    {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }
    @Suspendable
    private fun collectsignatures(transaction: SignedTransaction,session: List<FlowSession>):SignedTransaction
            = subFlow(CollectSignaturesFlow(transaction,session))
    @Suspendable
    private fun recordIssue(transaction: SignedTransaction,session: List<FlowSession>): SignedTransaction
            = subFlow(FinalityFlow(transaction,session))

    @InitiatedBy(TokenIssueFlow::class)
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