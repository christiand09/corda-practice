package com.template.flows.UserFlow

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.UserState
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.*
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

@InitiatingFlow
@StartableByRPC
class UserVerifyFlow(private val linearId: UniqueIdentifier, private val counterparty: String):FlowLogic<SignedTransaction>() {

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
        progressTracker.currentStep = CREATING
        val verify = verify()
        val counterRef = serviceHub.identityService.partiesFromName(counterparty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterparty.")
        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyandsign(transaction = verify)
        val session = initiateFlow(counterRef)
        val transactionSignedbyAllParties = collectSignatures(transaction = signedTransaction,session =  listOf(session))
        progressTracker.currentStep = FINALISING
        return recordVerify(transaction = transactionSignedbyAllParties, session = listOf(session))
    }
    private fun verifystate():UserState
    {
        val counterRef = serviceHub.identityService.partiesFromName(counterparty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterparty.")
        val input = inputStateref().state.data
        return UserState(input.name, ourIdentity, counterRef, verify = true,linearId = linearId)
    }
    private fun verify():TransactionBuilder
    {
        val counterRef = serviceHub.identityService.partiesFromName(counterparty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterparty.")
        val notary = inputStateref().state.notary
        val command = Command(UserContract.Commands.Verify(),
                listOf(ourIdentity.owningKey,counterRef.owningKey))
        val builder = TransactionBuilder(notary = notary)
        builder.addInputState(inputStateref())
        builder.addOutputState(state = verifystate(),contract = UserContract.ID_Contracts)
        builder.addCommand(command)
        return builder
    }
    private fun inputStateref():StateAndRef<UserState>
    {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<UserState>(criteria = criteria).states.first()
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
