package com.template.flows.CashFlows
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import com.template.states.MyState
import com.template.contracts.MyContract
import com.template.flows.FlowFunction
//import com.template.states.formSet
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class SelfIssueCashFlow(private val amount: Int,  private val linearId: UniqueIdentifier = UniqueIdentifier()): FlowFunction() {

    @Suspendable
    override fun call(): SignedTransaction {
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val spySession = initiateFlow(spy)
        spySession.send(false)
        val transaction: TransactionBuilder = transaction(spy)
        val signedTransaction: SignedTransaction = verifyAndSign(transaction)
        val sessions = emptyList<FlowSession>()
        val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, sessions)
        return recordTransaction(transactionSignedByAllParties, listOf(spySession))
    }

    private fun inputStateRef(): StateAndRef<MyState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<MyState>(criteria).states.single()
    }

    private fun outputState(): MyState {
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val input = inputStateRef().state.data
        return MyState(
                input.formSet,
                ourIdentity,
                ourIdentity,
                spy,
                wallet = input.wallet + amount,
                amountdebt = input.amountdebt,
                amountpaid = input.amountpaid,
                linearId = linearId,
                status = "Now issuing cash worth $amount on $ourIdentity",
                debtFree = input.debtFree,
                approvals = input.approvals
        )
    }
    private fun transaction(spy: Party): TransactionBuilder {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//        val input = inputStateRef().state.data
//        val MyState = MyState(firstName,lastName,age, gender,address, ourIdentity, ourIdentity)
                val anotherOutputState = outputState().copy(participants = listOf(ourIdentity, spy))
        val issueCommand = Command(MyContract.Commands.Issue(),ourIdentity.owningKey)
        val builder = TransactionBuilder(notary = notary )
        builder.addInputState(inputStateRef())
        builder.addOutputState(anotherOutputState, MyContract.IOU_CONTRACT_ID)
        builder.addCommand(issueCommand)
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
@InitiatedBy(SelfIssueCashFlow::class)
class SelfIssueCashFlowResponder(private val sessions: FlowSession) : FlowLogic<SignedTransaction>() {
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