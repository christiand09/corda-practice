//package com.template.flows
//
//import co.paralleluniverse.fibers.Suspendable
//import com.template.contracts.TokenContract
//import com.template.states.TokenState
//import net.corda.core.contracts.Command
//import net.corda.core.contracts.StateAndRef
//import net.corda.core.contracts.UniqueIdentifier
//import net.corda.core.contracts.requireThat
//import net.corda.core.flows.*
//import net.corda.core.node.services.queryBy
//import net.corda.core.node.services.vault.QueryCriteria
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.transactions.TransactionBuilder
//import net.corda.core.utilities.ProgressTracker
//
//
//@InitiatingFlow
//@StartableByRPC
//class SelfIssueFlow(private val walletBalance: Long ,private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {
//
//    override val progressTracker = ProgressTracker(
//            GENERATING_TRANSACTION,
//            VERIFYING_TRANSACTION,
//            SIGNING_TRANSACTION,
//            NOTARIZE_TRANSACTION,
//            FINALISING_TRANSACTION )
//
//    @Suspendable
//    override fun call() : SignedTransaction {
//        progressTracker.currentStep = GENERATING_TRANSACTION
//        val userRegister = SelfISSUE(outputState())
//        progressTracker.currentStep = VERIFYING_TRANSACTION
//        progressTracker.currentStep = SIGNING_TRANSACTION
//        val signedTransaction = verifyAndSign(transaction = userRegister)
//        val sessions = emptyList<FlowSession>() // empty because the owner's signature is just needed
//        val transactionSignedByParties = collectSignature(transaction = signedTransaction, sessions = sessions)
//
//
//        progressTracker.currentStep = FINALISING_TRANSACTION
//        return recordTransaction(transaction = transactionSignedByParties, sessions = sessions)
//    }
//    private fun inputStateRef(): StateAndRef<TokenState> {
//        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
//        return serviceHub.vaultService.queryBy<TokenState>(criteria).states.first()
//    }
//    private fun outputState(): TokenState {
//        val input = inputStateRef().state.data
////        return TokenState(amount = input.amount.plus(amount),borrower = ourIdentity,lender = ourIdentity,linearId = linearId)
//        return TokenState(amountIssued = 0,amountPaid = 0,borrower = ourIdentity,lender = ourIdentity,walletBalance = input.walletBalance.plus(walletBalance),linearId = linearId)
//
//    }
//
//    private fun SelfISSUE(state: TokenState): TransactionBuilder {
//        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//        val cmd = Command(TokenContract.Commands.SelfIssue(),ourIdentity.owningKey)
//        val txBuilder = TransactionBuilder(notary)
//                .addInputState(inputStateRef())
//                .addOutputState(state, TokenContract.tokenID)
//                .addCommand(cmd)
//        return txBuilder
//    }
//
//    private fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
//        transaction.verify(serviceHub)
//
//        return serviceHub.signInitialTransaction(transaction)
//    }
//
//    @Suspendable
//    private fun collectSignature(
//            transaction: SignedTransaction,
//            sessions: List<FlowSession>
//    ): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, sessions))
//
//    @Suspendable
//
//    private fun recordTransaction(transaction: SignedTransaction, sessions: List<FlowSession>): SignedTransaction =
//            subFlow(FinalityFlow(transaction, sessions))
//
//}
//
//
//@InitiatedBy(SelfIssueFlow::class)
//class SelfIssueFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {
//
//    @Suspendable
//    override fun call(): SignedTransaction {
//        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
//            override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                val output = stx.tx.outputs.single().data
//                "This must be an IOU transaction" using (output is TokenState)
//            }
//        }
//
//        val txWeJustSignedId = subFlow(signedTransactionFlow)
//
//        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
//    }
//}
