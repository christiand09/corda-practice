package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import kotlin.math.sign

@InitiatingFlow
@StartableByRPC
class UserVerifyFlow(private val id: UniqueIdentifier):FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(id))
        val inputState = serviceHub.vaultService.queryBy<UserState>(criteria).states.single()
        val input = inputState.state.data
        val receive = inputState.state.data.receiver
        val notary = inputState.state.notary
        val userState = UserState(input.fullname, input.age, input.gender, input.address, input.sender, input.receiver, true)
        val cmd = Command(UserContract.Commands.Verify(), listOf(receive.owningKey,ourIdentity.owningKey))
        val txBuilder = TransactionBuilder(notary = notary)
                .addInputState(inputState)
                .addOutputState(userState, UserContract.ID_Contracts)
                .addCommand(cmd)
        txBuilder.verify(serviceHub)
        val signedtx = serviceHub.signInitialTransaction(txBuilder)
        val session = initiateFlow(inputState.state.data.receiver)

        val ssss = subFlow(CollectSignaturesFlow(signedtx, listOf(session)))

        return subFlow(FinalityFlow(ssss, session))
    }

    @InitiatedBy(UserVerifyFlow::class)
    class VerifyFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an IOU transaction" using (output is UserState)
                }
            }

            val txWeJustSignedId = subFlow(signedTransactionFlow)

            return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
        }
    }
}
