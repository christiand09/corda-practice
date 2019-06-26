package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.ClientContract
import com.template.states.ClientState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class ClientFlow(val state: ClientState) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {


        val notary= serviceHub.networkMapCache.notaryIdentities.first()

        val issueCommand = Command(ClientContract.Commands.Register(), state.participants.map { it.owningKey })

        val builder = TransactionBuilder(notary)

        builder.addCommand(issueCommand)
        builder.addOutputState(state, ClientContract.ID)

        builder.verify(serviceHub)

        val sessions = (state.participants - ourIdentity).map { initiateFlow(it) }.toSet()

        val ptx = serviceHub.signInitialTransaction(builder)

        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))

        return subFlow(FinalityFlow(stx, sessions))





    }
}


/**
 * This is the flow which signs IOU issuances.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(ClientFlow::class)
class ClientFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction" using (output is ClientState)
            }
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}
