//package com.template.flows
//
//import co.paralleluniverse.fibers.Suspendable
//import com.template.contracts.MyContract
//import com.template.states.MyState
//import net.corda.core.contracts.Command
//import net.corda.core.flows.FlowLogic
//import net.corda.core.flows.InitiatingFlow
//import net.corda.core.flows.StartableByRPC
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.transactions.TransactionBuilder
//
//
//@InitiatingFlow
//@StartableByRPC
//
//class TryFlowOOP (val state: MyState): FlowLogic<SignedTransaction>(){
//    @Suspendable
//    override fun call(): SignedTransaction {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    private fun transaction(){
//        val notary =  serviceHub.networkMapCache.notaryIdentities.first()
//        val issueCommand = Command(MyContract.Commands.Issue, ourIdentity.owningKey)
//        val builder = TransactionBuilder
//    }
//
//
//
//}