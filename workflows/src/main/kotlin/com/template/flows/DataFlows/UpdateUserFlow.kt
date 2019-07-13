package com.template.flows.DataFlows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.MyContract
import com.template.flows.FlowFunction
import com.template.states.MyState
import com.template.states.formSet
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap


@InitiatingFlow
@StartableByRPC
class UpdateUserFlow( private val formSet: formSet,
                     private val receiver: String,
                      private val linearId: UniqueIdentifier = UniqueIdentifier()): FlowFunction(){

    @Suspendable
    override fun call():SignedTransaction {
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val spySession = initiateFlow(spy)
        spySession.send(false)
        val transaction: TransactionBuilder = transaction()
        val signedTransaction: SignedTransaction = verifyAndSign(transaction)
        val counterRef = serviceHub.identityService.partiesFromName(receiver, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $receiver.")
        val sessions = initiateFlow(counterRef)
        sessions.send(true)
        val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, listOf(sessions))
        return recordTransaction(transactionSignedByAllParties, listOf(sessions, spySession))
    }

    private fun inputStateRef(): StateAndRef<MyState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<MyState>(criteria).states.single()
    }

    private fun outputState(): MyState{
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val input = inputStateRef().state.data
        val counterRef = serviceHub.identityService.partiesFromName(receiver, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $receiver.")
        //return MyState(firstName,lastName,age,gender,address,ourIdentity,input.receiver,input.approvals, input.participants, input.linearId)
                return MyState(
                        formSet,
                        ourIdentity,
                        counterRef,
                        spy,
                        input.wallet,
                        input.amountdebt,
                        input.amountpaid,
                        "Updating user. From ${input.formSet} to $formSet",
                        input.debtFree,
                        input.approvals,
                        listOf(ourIdentity, counterRef, spy),
                        input.linearId
        )
    }

    private fun transaction(): TransactionBuilder {
//        val inputCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
//        val vault = serviceHub.vaultService.queryBy<MyState>(inputCriteria).states.first()
//        var input = vault.state.data
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//        val userState = MyState(firstName, lastName, age, gender, address, ourIdentity, input.receiver, input.approvals, input.participants, input.linearId)

        when {
            formSet.firstName == "" -> formSet.firstName = inputStateRef().state.data.formSet.firstName
        }
        when {
            formSet.lastName == "" -> formSet.lastName = inputStateRef().state.data.formSet.lastName
        }
        when {
            formSet.gender == "" -> formSet.gender = inputStateRef().state.data.formSet.gender
        }
        when {
            formSet.address == "" -> formSet.address = inputStateRef().state.data.formSet.address
        }
        when {
            formSet.age == "" -> formSet.age = inputStateRef().state.data.formSet.age
        }
        val cmd = Command(MyContract.Commands.Issue(),ourIdentity.owningKey)
        val builder = TransactionBuilder(notary)
        builder.addInputState(inputStateRef())
        builder.addOutputState(outputState(),MyContract.IOU_CONTRACT_ID)
        builder.addCommand(cmd)
        return builder
    }

    @Suspendable
    private fun recordTransaction(transaction: SignedTransaction, sessions: List<FlowSession>): SignedTransaction =
            subFlow(FinalityFlow(transaction, sessions))
}

@InitiatedBy(UpdateUserFlow::class)
class UpdateUserFlowResponder(private val sessions: FlowSession) : FlowLogic<SignedTransaction>() {

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