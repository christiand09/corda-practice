package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.MyContract
import com.template.contracts.MyContract.Companion.ID
import com.template.states.MyState
import com.template.states.Registered
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class MyUpdateVerifyFlow(
        private var registered: Registered,
        private val counterParty: String,
        private val linearId: UniqueIdentifier): FlowLogic<SignedTransaction>(){

    @Suspendable
    override fun call(): SignedTransaction {

        val unUpdated= userUpdate()
        val signedTransaction = verifyAndSign(unUpdated)
        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
        val sessions = initiateFlow(counterRef)
        val transactionSignedByAllParties: SignedTransaction=collectSignature(signedTransaction, listOf(sessions))
        sessions.send(registered)
        return recordTransaction(transactionSignedByAllParties, listOf(sessions))
    }
    private fun inputStateRef(): StateAndRef<MyState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))

        return serviceHub.vaultService.queryBy<MyState>(criteria).states.first()
    }
    private fun outputState(): MyState{
        val input = inputStateRef().state.data

        if(registered.firstName == "")
            registered.firstName = input.registered.firstName
        if(registered.lastName == "")
            registered.lastName = input.registered.lastName
        if(registered.age == "")
            registered.age = input.registered.age
        if(registered.birthDate == "")
            registered.birthDate = input.registered.birthDate
        if(registered.address == "")
            registered.address = input.registered.address
        if(registered.contactNumber == "")
            registered.contactNumber = input.registered.contactNumber.toString()
        if(registered.status == "")
            registered.status = input.registered.status

        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")

        return MyState(registered,ourIdentity,counterRef,true,input.linearId)

    }
    private fun userUpdate(): TransactionBuilder {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val cmd = Command(MyContract.Commands.Update(), outputState().participants.map { it.owningKey })
        return TransactionBuilder (notary)
                .addInputState(inputStateRef())
                .addOutputState(outputState(), ID)
                .addCommand(cmd)
    }
    private fun verifyAndSign(transaction : TransactionBuilder): SignedTransaction{
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

@InitiatedBy(MyUpdateVerifyFlow::class)
class MyResponderFlowUpdateVerify(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction" using (output is MyState)
            }
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)
        val payload = flowSession.receive(Registered::class.java).unwrap { it }
        subFlow(MyRegisterFlow(payload))
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}