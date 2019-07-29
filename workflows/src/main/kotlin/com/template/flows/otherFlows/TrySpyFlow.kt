package com.template.flows.otherFlows

//package com.template.flows
//
//import co.paralleluniverse.fibers.Suspendable
//import com.template.contracts.MyContract
//import com.template.states.MyState
//import com.template.states.formSet
//import net.corda.core.contracts.Command
//import net.corda.core.contracts.StateAndContract
//import net.corda.core.contracts.requireThat
//import net.corda.core.crypto.SecureHash
//import net.corda.core.flows.*
//import net.corda.core.identity.Party
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.transactions.TransactionBuilder
//import net.corda.core.contracts.StateAndRef
//import net.corda.core.contracts.UniqueIdentifier
//import net.corda.core.contracts.requireThat
//import net.corda.core.node.services.queryBy
//import net.corda.core.node.services.vault.QueryCriteria
//
//@InitiatingFlow
//class SendMessageFlow(private val message: MyState) :
//        FlowLogic<SignedTransaction>() {
//
//    @Suspendable
//    override fun call(): SignedTransaction {
//
//        val spy = serviceHub.identityService.partiesFromName("Spy", false).first()
//
//        val tx = verifyAndSign(transaction(spy))
//
//        // initiate sessions with each party
//        val signingSession = initiateFlow(message.receiver)
//        val spySession = initiateFlow(spy)
//
//        // send signing flags to counterparties
//        signingSession.send(true)
//        spySession.send(false)
//
//        val stx = collectSignature(tx, listOf(signingSession))
//
//        // tell everyone to save the transaction
//        return subFlow(FinalityFlow(stx, listOf(signingSession, spySession))
//    }
//
//    private fun transaction(spy: Party) =
//            TransactionBuilder(notary()).apply {
//                // the spy is added to the messages participants
//                val spiedOnMessage = message.copy(participants = message.participants + spy)
//                addOutputState(spiedOnMessage, MessageContract.CONTRACT_ID)
//                addCommand(Command(Send(), listOf(message.recipient, message.sender).map(Party::owningKey)))
//            }
//}
//
//@InitiatedBy(SendMessageFlow::class)
//class SendMessageResponder(private val session: FlowSession) : FlowLogic<SignedTransaction>() {
//
//    @Suspendable
//    override fun call(): SignedTransaction {
//        // receive the flag
//        val needsToSignTransaction = session.receive<Boolean>().unwrap { it }
//        // only sign if instructed to do so
//        if (needsToSignTransaction) {
//            subFlow(object : SignTransactionFlow(session) {
//                override fun checkTransaction(stx: SignedTransaction) { }
//            })
//        }
//        // always save the transaction
//        return subFlow(ReceiveFinalityFlow(otherSideSession = session))
//    }
//}