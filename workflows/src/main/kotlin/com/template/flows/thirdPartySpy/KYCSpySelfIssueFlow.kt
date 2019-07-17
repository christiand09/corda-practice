package com.template.flows.thirdPartySpy

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.KYCSpyContract
import com.template.states.KYCSpyState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class KYCSpySelfIssueFlow(private val amount: Long, val linearId: UniqueIdentifier): FlowLogic<SignedTransaction>(){

    @Suspendable
    override fun call(): SignedTransaction {
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val signedTransaction = verifyAndSign(transaction(spy))
        val spySession = initiateFlow(spy)
        val transactionSignedByAllParties = collectSignature(signedTransaction)

        return recordTransaction (transactionSignedByAllParties, listOf(spySession))
    }

    private fun inputStateRef(): StateAndRef<KYCSpyState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<KYCSpyState>(criteria).states.single()
    }

    private fun outputState(): KYCSpyState{
        val input = inputStateRef().state.data
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()

        return KYCSpyState(moneyLend=input.moneyLend,moneyBalance=input.moneyBalance.plus(amount),requestedAmount=input.requestedAmount,lender=input.lender,borrower=input.borrower,spy = spy, status = "Deposit $amount into moneyBalance", linearId = input.linearId)
    }
    private fun transaction(spy: Party): TransactionBuilder {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val spiedOnMessage = outputState().copy(participants = outputState().participants + spy)
        val kycCommand = Command (KYCSpyContract.Commands.SelfIssue(), ourIdentity.owningKey)
        return TransactionBuilder(notary)
                .addInputState(inputStateRef())
                .addOutputState(spiedOnMessage,KYCSpyContract.ID)
                .addCommand(kycCommand)
    }
    private fun verifyAndSign (transactionBuilder: TransactionBuilder): SignedTransaction {
        transactionBuilder.verify(serviceHub)
        return serviceHub.signInitialTransaction(transactionBuilder)
    }

    @Suspendable
    private fun collectSignature(
            transaction: SignedTransaction
    ): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, emptyList()))

    @Suspendable
    private fun recordTransaction(
            transaction: SignedTransaction,
            sessions: List<FlowSession>
    ): SignedTransaction = subFlow(FinalityFlow(transaction, sessions))


}

@InitiatedBy(KYCSpySelfIssueFlow::class)
class SpySelfIssue(private val session: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        return subFlow(ReceiveFinalityFlow(otherSideSession = session))
    }
}