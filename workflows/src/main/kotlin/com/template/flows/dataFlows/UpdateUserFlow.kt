package com.template.flows.dataFlows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.MyContract
import com.template.flows.*
import com.template.states.MyState
import com.template.states.formSet
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap


@InitiatingFlow
@StartableByRPC
class UpdateUserFlow(private val formSet: formSet,
                     private val receiver: String,
                     private val linearId: UniqueIdentifier = UniqueIdentifier()): FlowFunction(){
    override val progressTracker = ProgressTracker(INITIALIZING, BUILDING, SIGNING, COLLECTING, FINALIZING)

    @Suspendable
    override fun call():SignedTransaction {
        progressTracker.currentStep = INITIALIZING
        val spy = stringToPartySpy("PartyC")
        val spySession = initiateFlow(spy)
        spySession.send(false)
        val transaction: TransactionBuilder = transaction()
        val signedTransaction: SignedTransaction = verifyAndSign(transaction)
        val counterRef = stringToParty(receiver)
        val sessions = initiateFlow(counterRef)
        sessions.send(true)
        val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, listOf(sessions))
        return recordTransaction(transactionSignedByAllParties, listOf(sessions, spySession))
    }

    private fun outputState(): MyState{
        val spy = stringToPartySpy("PartyC")
        val input = inputStateRef(linearId).state.data
        val counterRef = stringToParty(receiver)
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
        progressTracker.currentStep = BUILDING
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        when {
            formSet.firstName == "" -> formSet.firstName = inputStateRef(linearId).state.data.formSet.firstName
        }
        when {
            formSet.lastName == "" -> formSet.lastName = inputStateRef(linearId).state.data.formSet.lastName
        }
        when {
            formSet.gender == "" -> formSet.gender = inputStateRef(linearId).state.data.formSet.gender
        }
        when {
            formSet.address == "" -> formSet.address = inputStateRef(linearId).state.data.formSet.address
        }
        when {
            formSet.age == "" -> formSet.age = inputStateRef(linearId).state.data.formSet.age
        }
        val cmd = Command(MyContract.Commands.Issue(),ourIdentity.owningKey)
        val builder = TransactionBuilder(notary)
        builder.addInputState(inputStateRef(linearId))
        builder.addOutputState(outputState(),MyContract.IOU_CONTRACT_ID)
        builder.addCommand(cmd)
        return builder
    }


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