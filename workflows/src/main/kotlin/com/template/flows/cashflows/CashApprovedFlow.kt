//package com.template.flows.cashflows
//
//import co.paralleluniverse.fibers.Suspendable
//import com.template.contracts.WalletContract
//import com.template.states.WalletState
//import net.corda.core.contracts.Command
//import net.corda.core.contracts.StateAndRef
//import net.corda.core.contracts.UniqueIdentifier
//import net.corda.core.contracts.requireThat
//import net.corda.core.flows.*
//import net.corda.core.flows.CollectSignaturesFlow
//import net.corda.core.flows.FinalityFlow
//import net.corda.core.flows.ReceiveFinalityFlow
//import net.corda.core.node.services.queryBy
//import net.corda.core.node.services.vault.QueryCriteria
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.transactions.TransactionBuilder
//import net.corda.core.utilities.ProgressTracker
//
//@InitiatingFlow
//@StartableByRPC
//class CashApprovedFlow (private val counterParty: String,
//                        private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>()
//{
//    override val progressTracker: ProgressTracker = tracker()
//
//    companion object
//    {
//        object CREATING : ProgressTracker.Step("Creating registration!")
//        object SIGNING : ProgressTracker.Step("Signing registration!")
//        object VERIFYING : ProgressTracker.Step("Verifying registration!")
//        object NOTARIZING : ProgressTracker.Step("Notarizing registration!")
//        object FINALISING : ProgressTracker.Step("Finalize registration!")
//        {
//            override fun childProgressTracker() = FinalityFlow.tracker()
//        }
//
//        fun tracker() = ProgressTracker(CREATING, SIGNING, VERIFYING, NOTARIZING, FINALISING)
//    }
//
//    @Suspendable
//    override fun call(): SignedTransaction
//    {
//        progressTracker.currentStep = CREATING
//        val approval = approved()
//
//        progressTracker.currentStep = VERIFYING
//        progressTracker.currentStep = SIGNING
//        val signedTransaction = verifyAndSign(transaction = approval)
//        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
//                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
//        val sessions = initiateFlow(counterRef)
//        val transactionSignedByParties = collectSignature(transaction = signedTransaction, sessions = listOf(sessions))
//
//        progressTracker.currentStep = NOTARIZING
//        progressTracker.currentStep = FINALISING
//        return recordRegistration(transaction = transactionSignedByParties, sessions = listOf(sessions))
//    }
//
//    private fun inputStateRef(): StateAndRef<WalletState> {
//        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
//        return serviceHub.vaultService.queryBy<WalletState>(criteria = criteria).states.single()
//    }
//
//    private fun outState(): WalletState
//    {
//        val input = inputStateRef().state.data
//        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
//                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
//
//        if(input.wallet.toInt() == 0)
//        {
//            return WalletState(
//                    amount = 0,
//                    request = false,
//                    status = input.status,
//                    borrower = input.borrower,
//                    lender = counterRef,
//                    wallet = input.wallet,
//                    linearId = linearId,
//                    participants = listOf(counterRef, counterRef)
//            )
//        }
//        else
//        {
//            return WalletState(
//                    amount = input.amount,
//                    request = true,
//                    status = input.status,
//                    borrower = input.borrower,
//                    lender = input.lender,
//                    wallet = input.wallet,
//                    linearId = linearId
//            )
//        }
//    }
//
//    private fun approved(): TransactionBuilder
//    {
//        val notary = inputStateRef().state.notary
//        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
//                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
//
//        val issueCommand =
//                if(!outState().request)
//                {
//                    Command(WalletContract.Commands.Approved(), counterRef.owningKey)
//                }
//                else
//                {
//                    Command(WalletContract.Commands.Approved(), listOf(ourIdentity.owningKey, counterRef.owningKey))
//                }
//
//        return TransactionBuilder(notary = notary)
//                .addInputState(inputStateRef())
//                .addOutputState(outState(), WalletContract.WALLET_ID)
//                .addCommand(issueCommand)
//    }
//
//    private fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
//        transaction.verify(serviceHub)
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
//    private fun recordRegistration(
//            transaction: SignedTransaction,
//            sessions: List<FlowSession>
//    ): SignedTransaction = subFlow(FinalityFlow(transaction, sessions))
//}
//
//@InitiatedBy(CashApprovedFlow::class)
//class CashApprovedFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
//{
//    @Suspendable
//    override fun call(): SignedTransaction
//    {
//        val signTransactionFlow = object : SignTransactionFlow(flowSession)
//        {
//            override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                val output = stx.tx.outputs.single().data
//                "This must be an issue transaction" using (output is WalletState)
//            }
//        }
//        val signedTransaction = subFlow(signTransactionFlow)
//        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
//    }
//}