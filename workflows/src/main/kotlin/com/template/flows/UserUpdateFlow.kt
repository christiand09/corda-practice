package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
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
import com.template.states.UserDetails

@InitiatingFlow
@StartableByRPC
class UserUpdateFlow (private var name: UserDetails,
                      private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val update = update()
        val signedTransaction = verifyandsign(update)
        val session = (updatestate().participants - ourIdentity).map { initiateFlow(it) }.toSet().toList()
        val transactionsignedbyall = collectsignatures(signedTransaction,session)
        return recordUpdate(transactionsignedbyall,session)

    }
    private fun updatestate():UserState
    {
        val input = inputstateref().state.data
        if (name.fullname == "") name.fullname = input.name.fullname
        if (name.age == "") name.age = input.name.age
        if (name.gender == "") name.gender = input.name.gender
        if (name.address == "") name.address = input.name.address
        return UserState(name, ourIdentity, input.receiver, input.verify, linearId = linearId)
    }
    private fun inputstateref(): StateAndRef<UserState>
    {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<UserState>(criteria).states.single()
    }
    private fun update(): TransactionBuilder
    {
        val notary = inputstateref().state.notary
        val input = inputstateref().state.data
        val command = Command(UserContract.Commands.Update(), listOf(input.receiver.owningKey, ourIdentity.owningKey))
        val builder = TransactionBuilder(notary = notary)
                .addInputState(inputstateref())
                .addOutputState(updatestate(),UserContract.ID_Contracts)
                .addCommand(command)
        return builder
    }
    private fun verifyandsign(transaction: TransactionBuilder): SignedTransaction
    {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }
    @Suspendable
    private fun collectsignatures(transaction: SignedTransaction,session: List<FlowSession>):SignedTransaction
     = subFlow(CollectSignaturesFlow(transaction,session))

    @Suspendable
    private fun recordUpdate(transaction: SignedTransaction,session: List<FlowSession>):SignedTransaction
            = subFlow(FinalityFlow(transaction,session))

    @InitiatedBy(UserUpdateFlow::class)
    class IOUIssueFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

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