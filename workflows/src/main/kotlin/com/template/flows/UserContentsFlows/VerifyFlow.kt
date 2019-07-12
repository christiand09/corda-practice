package com.template.flows.UserContentsFlows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.flows.*
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class VerifyFlow (private val receiver: String,
                  private val linearId: UniqueIdentifier) : FlowFunctions(){


    override val progressTracker = ProgressTracker(
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            NOTARIZE_TRANSACTION,
            FINALISING_TRANSACTION)

    @Suspendable
    override fun call() : SignedTransaction {

        progressTracker.currentStep = GENERATING_TRANSACTION
        val unverified = userVerified()

        progressTracker.currentStep = VERIFYING_TRANSACTION
        progressTracker.currentStep = SIGNING_TRANSACTION
        val signedTransaction = verifyAndSign(transaction = unverified)

        val counterRef = stringToParty(receiver)
        val sessions = initiateFlow(counterRef)
        val allsignature = collectSignature(signedTransaction, listOf(sessions))
        progressTracker.currentStep = FINALISING_TRANSACTION

        return recordTransaction(transaction = allsignature, sessions = listOf(sessions))


    }


    private fun outputState(): UserState{
        val counterRef = stringToParty(receiver)
        val input = inputStateRef(linearId).state.data
        return UserState(input.name,sender = ourIdentity,receiver = counterRef,verify = true,linearId = input.linearId)

    }
    private fun userVerified(): TransactionBuilder{
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val cmd = Command(UserContract.Commands.Verify(), outputState().participants.map { it.owningKey })
        val txBuilder = TransactionBuilder(notary)
                .addInputState(inputStateRef(linearId))
                .addOutputState(outputState(),UserContract.REGISTER_ID)
                .addCommand(cmd)
        return txBuilder
    }
    @InitiatedBy(VerifyFlow::class)
    class VerifyFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

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