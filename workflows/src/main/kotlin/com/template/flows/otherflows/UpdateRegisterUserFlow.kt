package com.template.flows.otherflows
//
//import co.paralleluniverse.fibers.Suspendable
//import com.template.contracts.MyContract
//import com.template.flows.DataFlows.RegisterUserFlow
//import com.template.states.MyState
//import com.template.states.formSet
//import net.corda.core.contracts.Command
//import net.corda.core.contracts.StateAndRef
//import net.corda.core.contracts.UniqueIdentifier
//import net.corda.core.flows.*
//import net.corda.core.identity.Party
//import net.corda.core.node.services.queryBy
//import net.corda.core.node.services.vault.QueryCriteria
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.transactions.TransactionBuilder
//import net.corda.core.utilities.unwrap
//
////Update current User Info then send a copy to receiver Party as new state with new linearId
//@InitiatingFlow
//@StartableByRPC
//class UpdateRegisterUserFlow(
//                     private val formSet: formSet,
//                     private val receiver: String,
//                     private val linearId: UniqueIdentifier = UniqueIdentifier()): FlowLogic<SignedTransaction>(){
//
//    @Suspendable
//    override fun call(): SignedTransaction {
//        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
//        val spySession = initiateFlow(spy)
//        spySession.send(false)
//        val transaction: TransactionBuilder = transaction(spy)
//        val signedTransaction: SignedTransaction = verifyAndSign(transaction)
////        val sessions = (outputState().participants - ourIdentity).map { initiateFlow(it) }.toList()
//        val counterRef = serviceHub.identityService.partiesFromName(receiver, false).singleOrNull()
//                ?: throw IllegalArgumentException("No match found for Owner $receiver.")
//        val sessions = initiateFlow(counterRef)
//        sessions.send(formSet)
//        val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, listOf(sessions))
//        return recordTransaction(transactionSignedByAllParties, listOf(sessions, spySession))
//    }
//
//    private fun inputStateRef(): StateAndRef<MyState> {
//        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
//        return serviceHub.vaultService.queryBy<MyState>(criteria).states.single()
//    }
//
//    private fun outputState(): MyState {
//        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
//        val input = inputStateRef().state.data
//        //return MyState(firstName,lastName,age,gender,address,ourIdentity,input.receiver,input.approvals, input.participants, input.linearId)
//        val counterRef = serviceHub.identityService.partiesFromName(receiver, false).singleOrNull()
//                ?: throw IllegalArgumentException("No match found for Owner $receiver.")
//        when {
//            formSet.firstName == "" -> formSet.firstName = inputStateRef().state.data.formSet.firstName
//        }
//        when {
//            formSet.lastName == "" -> formSet.lastName = inputStateRef().state.data.formSet.lastName
//        }
//        when {
//            formSet.gender == "" -> formSet.gender = inputStateRef().state.data.formSet.gender
//        }
//        when {
//            formSet.address == "" -> formSet.address = inputStateRef().state.data.formSet.address
//        }
//        when {
//            formSet.age == "" -> formSet.age = inputStateRef().state.data.formSet.age
//        }
//
//        return MyState(
//                formSet,
//                ourIdentity,
//                counterRef,
//                spy,
//                input.wallet,
//                input.amountdebt,
//                input.amountpaid,
//                "User updated and was registered on $counterRef",
//                input.debtFree,
//                input.approvals,
//                listOf(ourIdentity,counterRef,spy),
//                linearId = linearId
//        )
//    }
//
//    private fun transaction(spy:Party): TransactionBuilder {
////        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//        val cmd = Command(MyContract.Commands.Issue(), (outputState().participants-spy).map { it.owningKey })
//        val builder = TransactionBuilder(notary = notary)
//        builder.addInputState(inputStateRef())
//        builder.addOutputState(outputState(), MyContract.IOU_CONTRACT_ID)
//        builder.addCommand(cmd)
//        return builder
////        val userState = MyState(firstName, lastName, age, gender, address, ourIdentity, input.receiver, input.approvals, input.participants, input.linearId)
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
//    private fun recordTransaction(transaction: SignedTransaction, sessions: List<FlowSession>): SignedTransaction =
//            subFlow(FinalityFlow(transaction, sessions))
//}
//
//
//@InitiatedBy(UpdateRegisterUserFlow::class)
//class UpdateRegisterUserFlowResponder(private val sessions: FlowSession) : FlowLogic<SignedTransaction>() {
//
//    @Suspendable
//    override fun call(): SignedTransaction {
//        // receive the flag
//        val payload = sessions.receive(formSet::class.java).unwrap {it}
//        subFlow(RegisterUserFlow(payload))
//        val needsToSignTransaction = sessions.receive<Boolean>().unwrap { it }
//        // only sign if instructed to do so
//        if (needsToSignTransaction) {
//            subFlow(object : SignTransactionFlow(sessions) {
//                override fun checkTransaction(stx: SignedTransaction) { }
//            })
//        }
//
//        // always save the transaction
//        return subFlow(ReceiveFinalityFlow(otherSideSession = sessions))
//    }
//}
//
//
////@InitiatedBy(UpdateRegisterUserFlow::class)
////class UpdateRegisterUserFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {
////    @Suspendable
////    override fun call(): SignedTransaction {
////        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
////            override fun checkTransaction(stx: SignedTransaction) = requireThat {
////                val output = stx.tx.outputs.single().data
////                "This must be an IOU transaction" using (output is MyState)
////            }
////        }
////
////        val payload = flowSession.receive(formSet::class.java).unwrap {it}
////        val txWeJustSignedId = subFlow(signedTransactionFlow)
////        subFlow(RegisterUserFlow(payload))
////        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
////    }
////}