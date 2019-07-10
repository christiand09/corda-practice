//package com.template.flows
//
//
//import co.paralleluniverse.fibers.Suspendable
//import com.template.contracts.MyContract
//import net.corda.core.flows.*
//import net.corda.core.identity.Party
//import net.corda.core.node.services.queryBy
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.transactions.TransactionBuilder
//import com.template.states.MyState
//import net.corda.core.contracts.Command
//import net.corda.core.contracts.UniqueIdentifier
//import net.corda.core.node.services.vault.QueryCriteria
//import java.security.PublicKey
//
//
//
//@StartableByRPC
//@InitiatingFlow
//class TransferTokenFlow(private val receiver: Party,
//                        private val amountToTransfer: Int,
//                        private val linearId: UniqueIdentifier = UniqueIdentifier()):FlowLogic<SignedTransaction>(){
//
//    @Suspendable
//    override fun call(): SignedTransaction {
//        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//
//        // Try to combine existing tokens before transferring to receiver
////        subFlow(CombineTokensFlow())
//        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
//        val inputTokenStateReference = serviceHub.vaultService.queryBy<MyState>(criteria).states.first()
//        val inputTokenState = inputTokenStateReference.state.data
//
//        val updatedInputTokenState = inputTokenState.copy(participants = listOf(ourIdentity), cash = inputTokenState.cash - amountToTransfer )
//        val updatedOutputTokenState = inputTokenState.copy(participants = listOf(receiver), cash = amountToTransfer)
////        val updatedOwnerState = updatedOutputTokenState.withNewOwner(receiver)
//        val signers: List<PublicKey> = listOf(inputTokenState.sender.owningKey, inputTokenState.receiver.owningKey)
//        val issueCommand = Command(MyContract.Commands.Issue(),signers)
//        val transactionBuilder = TransactionBuilder(notary)
//                .addInputState(inputTokenStateReference)
//                .addOutputState(updatedOutputTokenState, MyContract.IOU_CONTRACT_ID)
//                .addOutputState(updatedInputTokenState, MyContract.IOU_CONTRACT_ID)
//                .addCommand(issueCommand)
//        transactionBuilder.verify(serviceHub)
//        val partiallySignedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
//        val otherPartySession = initiateFlow(receiver)
//        val fullySignedTransaction = subFlow(CollectSignaturesFlow(partiallySignedTransaction, listOf(otherPartySession)))
//        return subFlow(FinalityFlow(fullySignedTransaction, listOf(otherPartySession)))
//    }
//
//}
//
//@InitiatedBy(TransferTokenFlow::class)
//class TransferTokenResponderFlow(val otherPartySession: FlowSession): FlowLogic<Unit>(){
//    @Suspendable
//    override fun call() {
//        subFlow(object: SignTransactionFlow(otherPartySession){
//            override fun checkTransaction(stx: SignedTransaction) {
//                // sanity checks on this transaction
//            }})
//    }
//}