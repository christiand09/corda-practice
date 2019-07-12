package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import com.template.states.MyState
import com.template.contracts.MyContract
import com.template.states.formSet
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.unwrap


@InitiatingFlow
@StartableByRPC
class RegisterUserFlow(private val formSet: formSet
                     ): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val spySession = initiateFlow(spy)
        spySession.send(false)
        val transaction: TransactionBuilder = transaction(spy)
        val signedTransaction: SignedTransaction = verifyAndSign(transaction)
        val sessions = emptyList<FlowSession>()
        val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, sessions)
        return recordTransaction(transactionSignedByAllParties, listOf(spySession))
    }



    private fun outputState(): MyState
    {val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        return MyState(
                formSet,
                ourIdentity,
                ourIdentity,
                spy,
                wallet = 0,
                amountdebt = 0,
                amountpaid = 0,
                status = "${formSet.firstName} is now registered but not yet Approved",
                approvals = false
        )
    }

    private fun transaction(spy:Party): TransactionBuilder {

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//      val MyState = MyState(firstName,lastName,age, gender,address, ourIdentity, ourIdentity)
        val issueCommand = Command(MyContract.Commands.Issue(),ourIdentity.owningKey)
        val spiedOnMessage = outputState().copy(participants = listOf(ourIdentity, spy))
        val builder = TransactionBuilder(notary = notary )
        builder.addOutputState(spiedOnMessage, MyContract.IOU_CONTRACT_ID)
        builder.addCommand(issueCommand)
        return builder
    }

    private fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    @Suspendable
    private fun collectSignature(
            transaction: SignedTransaction,
            sessions: List<FlowSession>
    ): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, sessions))

    @Suspendable
    private fun recordTransaction(transaction: SignedTransaction, sessions: List<FlowSession>): SignedTransaction =
            subFlow(FinalityFlow(transaction, sessions))
}

/**
 * This is the flow which signs IOU issuances.
 * The signing is handled by the [SignTransactionFlow].
 */
//@InitiatedBy(RegisterUserFlow::class)
//class RegisterUserFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {
//
//    @Suspendable
//    override fun call(): SignedTransaction {
//        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
//            override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                val output = stx.tx.outputs.single().data
//                "This must be an IOU transaction" using (output is MyState)
//            }
//        }
//
//        val txWeJustSignedId = subFlow(signedTransactionFlow)
//
//        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
//    }
//}

@InitiatedBy(RegisterUserFlow::class)
class RegisterUserFlowResponder(private val sessions: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // receive the flag
        val needsToSignTransaction = sessions.receive<Boolean>().unwrap { it }
        // only sign if instructed to do so
        if (needsToSignTransaction) {
            subFlow(object : SignTransactionFlow(sessions) {
                override fun checkTransaction(stx: SignedTransaction) { }
            })
        }
        // always save the transaction
        return subFlow(ReceiveFinalityFlow(otherSideSession = sessions))
    }
}