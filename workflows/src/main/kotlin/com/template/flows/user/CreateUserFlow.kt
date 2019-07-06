package com.template.flows.user

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.Name
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class CreateUserFlow(private val name: Name,
                     private val age: Int) : UserBaseFlow() {
    @Suspendable
    override fun call(): SignedTransaction {
        val transaction = transaction()
        val signedTransaction = verifyAndSign(transaction)
        return recordTransaction(signedTransaction, emptyList())
//        val sessions = (state.participants - ourIdentity).map { initiateFlow(it) }.toSet().toList()
//        val transactionSignedByAllParties = collectSignature(signedTransaction, sessions)
//        return recordTransaction(transactionSignedByAllParties, sessions)
    }

    private fun transaction(): TransactionBuilder {
        val notary = firstNotary

        val output = UserState(name, age, participants = listOf(ourIdentity))
        val issueCommand =
                Command(UserContract.Commands.Create(), output.participants.map { it.owningKey })
        val builder = TransactionBuilder(notary = notary)
        builder.addOutputState(output, UserContract.USER_CONTRACT_ID)
        builder.addCommand(issueCommand)
        return builder
    }

}
//
//@InitiatedBy(CreateUserFlow::class)
//class CreateUserFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {
//
//    @Suspendable
//    override fun call(): SignedTransaction {
//        val signTransactionFlow = object : SignTransactionFlow(flowSession) {
//            override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                val output = stx.tx.outputs.single().data
//                "This must be an user transaction" using (output is UserState)
//            }
//        }
//        val signedTransaction = subFlow(signTransactionFlow)
//        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
//    }
//}