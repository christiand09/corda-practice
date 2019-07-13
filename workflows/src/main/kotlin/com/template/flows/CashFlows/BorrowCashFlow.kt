package com.template.flows.CashFlows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.MyContract
import com.template.flows.FlowFunction
import com.template.states.MyState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.unwrap

/**
 * This is the flow which handles issuance of new IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@InitiatingFlow
@StartableByRPC
class BorrowCashFlow( private val receiver: String,
                      private val amount: Int,
                      private val linearId: UniqueIdentifier = UniqueIdentifier()): FlowFunction() {

    @Suspendable
    override fun call(): SignedTransaction {
        if (!outputState().approvals){
             throw IllegalArgumentException("${outputState().formSet.firstName} is not yet a verified user.")
        }
        else {
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val spySession = initiateFlow(spy)
        spySession.send(false)
        val transaction: TransactionBuilder = transaction(spy)
        val signedTransaction: SignedTransaction = verifyAndSign(transaction)
        val counterRef = serviceHub.identityService.partiesFromName(receiver, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $receiver.")
        val sessions = initiateFlow(counterRef)
        sessions.send(true)
        val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, listOf(sessions))
        return recordTransaction(transactionSignedByAllParties, listOf(sessions, spySession))
    }}

    private fun inputStateRef(): StateAndRef<MyState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<MyState>(criteria).states.single()
    }

    private fun outputState(): MyState {
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val input = inputStateRef().state.data
        //return MyState(input.firstName,input.lastName,input.age,input.gender,input.address,ourIdentity, receiver, true,linearId = linearId)
        val counterRef = serviceHub.identityService.partiesFromName(receiver, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $receiver.")
        return MyState(
                input.formSet,
                ourIdentity,
                counterRef,
                spy,
                input.wallet,
                input.amountdebt,
                input.amountpaid,
                "Borrowing $amount from $counterRef",
                debtFree = input.debtFree,
                approvals = input.approvals,
                linearId = linearId
        )
    }

    private fun transaction(spy: Party): TransactionBuilder {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val cmd = Command(MyContract.Commands.Verify(),  (outputState().participants - spy).map { it.owningKey })
        val anotherOutputState = outputState().copy(participants = listOf(ourIdentity, outputState().receiver, spy))
        val builder = TransactionBuilder(notary = notary)
        builder.addInputState(inputStateRef())
        builder.addOutputState(anotherOutputState, MyContract.IOU_CONTRACT_ID)
        builder.addCommand(cmd)
        return builder
    }

        @Suspendable
        private fun recordTransaction(transaction: SignedTransaction, sessions: List<FlowSession>): SignedTransaction =
                subFlow(FinalityFlow(transaction, sessions))
}
/**
 * This is the flow which signs IOU issuances.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(BorrowCashFlow::class)
class BorrowCashFlowResponder(private val sessions: FlowSession) : FlowLogic<SignedTransaction>() {

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