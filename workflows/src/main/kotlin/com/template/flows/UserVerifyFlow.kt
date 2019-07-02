package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker.Step
import net.corda.core.utilities.ProgressTracker
import javax.jws.soap.SOAPBinding
import kotlin.math.sign

@InitiatingFlow
@StartableByRPC
class UserVerifyFlow(private val id: UniqueIdentifier, private val counterparty: Party):FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker = tracker()
    companion object
    {
        object CREATING : Step("Creating registration!")
        object SIGNING : Step("Signing registration!")
        object VERIFYING : Step("Verifying registration!")
        object FINALISING : Step("Finalize registration!")
        {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(CREATING, SIGNING, VERIFYING, FINALISING)
    }
    @Suspendable
    override fun call(): SignedTransaction {
//        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(id))
//        val inputState = serviceHub.vaultService.queryBy<UserState>(criteria).states.single()
//        val input = inputState.state.data
        //input.
//        val notary = inputState.state.notary
//        val userState = UserState(input.fullname, input.age, input.gender, input.address, input.sender, counterparty, true,linearId = id )
//        val cmd = Command(UserContract.Commands.Verify(), listOf(counterparty.owningKey,ourIdentity.owningKey))
//        val txBuilder = TransactionBuilder(notary = notary)
//                .addInputState(inputState)
//                .addOutputState(userState, UserContract.ID_Contracts)
//                .addCommand(cmd)
//        txBuilder.verify(serviceHub)
//        val signedtx = serviceHub.signInitialTransaction(txBuilder)
//        val session = initiateFlow(counterparty)
//
//        val ssss = subFlow(CollectSignaturesFlow(signedtx, listOf(session)))
//
//        return subFlow(FinalityFlow(ssss, session))
        progressTracker.currentStep = CREATING
        val verify = verify()
        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyandsign(verify)
        val session = (verifystate().participants - ourIdentity).map { initiateFlow(it) }.toSet().toList()
        val transactionSignedbyAllParties = collectSignatures(signedTransaction,session)
        progressTracker.currentStep = FINALISING
        return recordVerify(transactionSignedbyAllParties,session)
    }
    private fun verifystate():UserState
    {
        val input = inputStateref().state.data
        return UserState(input.fullname, input.age, input.gender, input.address, ourIdentity, counterparty, verify = true,linearId = id)
    }
    private fun verify():TransactionBuilder
    {
        val notary = inputStateref().state.notary
        val command = Command(UserContract.Commands.Verify(),
                listOf(counterparty.owningKey,ourIdentity.owningKey))
        val builder = TransactionBuilder(notary = notary)
        builder.addInputState(inputStateref())
        builder.addOutputState(state = verifystate(),contract = UserContract.ID_Contracts)
        builder.addCommand(command)
        return builder
    }
    private fun inputStateref():StateAndRef<UserState>
    {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(id))
        return serviceHub.vaultService.queryBy<UserState>(criteria = criteria).states.single()
    }
    private fun verifyandsign(transaction: TransactionBuilder): SignedTransaction
    {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }
    @Suspendable
    private fun collectSignatures(transaction: SignedTransaction,session: List<FlowSession>): SignedTransaction
            = subFlow(CollectSignaturesFlow(transaction,session))
    @Suspendable
    private fun recordVerify(transaction: SignedTransaction,session: List<FlowSession>): SignedTransaction
            = subFlow(FinalityFlow(transaction,session))

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
