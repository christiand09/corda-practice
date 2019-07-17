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
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class KYCSpySettleFlow (private val amountPay: Long
                     , private val lender: String
                     , val linearId: UniqueIdentifier): FlowLogic<SignedTransaction>(){


    @Suspendable
    override fun call(): SignedTransaction {
        if (outputState().status=="Paid") {
            val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
            val spySession = initiateFlow(spy)
            spySession.send(true)

            val counterRef = serviceHub.identityService.partiesFromName(lender, false).singleOrNull()
                    ?: throw IllegalArgumentException("No match found for Owner $lender.")
            val sessions = initiateFlow(counterRef)
            sessions.send(false)

            val signedTransaction = verifyAndSign(transaction(spy))
            val transactionSignedByAllParties = collectSignature(signedTransaction, listOf(spySession))

            return recordRegistration(transactionSignedByAllParties, listOf(sessions, spySession))
        }else {
            val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
            val spySession = initiateFlow(spy)
            spySession.send(false)

            val counterRef = serviceHub.identityService.partiesFromName(lender, false).singleOrNull()
                    ?: throw IllegalArgumentException("No match found for Owner $lender.")
            val sessions = initiateFlow(counterRef)
            sessions.send(true)

            val signedTransaction = verifyAndSign(transaction(spy))
            val transactionSignedByAllParties = collectSignature(signedTransaction, listOf(sessions))

            return recordRegistration(transactionSignedByAllParties, listOf(sessions, spySession))
        }
    }



    private fun inputStateRef() : StateAndRef<KYCSpyState> {

        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<KYCSpyState>(criteria).states.single()

    }

    private fun outputState() : KYCSpyState {
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val input = inputStateRef().state.data
        val counterRef = serviceHub.identityService.partiesFromName(lender, false).singleOrNull()
                ?:throw IllegalArgumentException("No match found for Owner $lender.")


        if(input.moneyLend < amountPay) throw FlowException("You only owed ${input.moneyLend} but gives $amountPay which is more than the amount owed")
        return if(input.moneyLend > amountPay)

        {
            KYCSpyState(moneyLend = input.moneyLend.minus(amountPay), moneyBalance = input.moneyBalance.minus(amountPay),requestedAmount = input.requestedAmount,
                    lender = counterRef,borrower = ourIdentity,spy = spy, status = "PartialPaid",linearId = input.linearId)
        }
        else {
            KYCSpyState(moneyLend = input.moneyLend.minus(amountPay), moneyBalance = input.moneyBalance.minus(amountPay),requestedAmount = input.requestedAmount,
                    lender = counterRef,borrower = ourIdentity,spy = spy ,status = "Paid",linearId = input.linearId)

        }



    }

    private fun transaction(spy: Party): TransactionBuilder {
        val notary = inputStateRef().state.notary
        val counterRef = serviceHub.identityService.partiesFromName(lender, false).singleOrNull()
                ?:throw IllegalArgumentException("No match found for Owner $lender.")
        return if (outputState().status=="PartialPaid"){
            val spiedOnMessage = outputState().copy(participants = outputState().participants + spy)
            val settleCommand = Command(KYCSpyContract.Commands.Settle(), listOf(ourIdentity.owningKey,counterRef.owningKey))
            TransactionBuilder(notary)
                    .addInputState(inputStateRef())
                    .addOutputState(spiedOnMessage,KYCSpyContract.ID)
                    .addCommand(settleCommand)
        }else{
            val spiedOnMessage = outputState().copy(participants = outputState().participants + spy - outputState().borrower - outputState().lender)
            val settleCommand = Command(KYCSpyContract.Commands.Settle(), listOf(spy.owningKey))
            TransactionBuilder(notary)
                    .addInputState(inputStateRef())
                    .addOutputState(spiedOnMessage,KYCSpyContract.ID)
                    .addCommand(settleCommand)
        }


    }

    private fun verifyAndSign (transactionBuilder: TransactionBuilder): SignedTransaction{
        transactionBuilder.verify(serviceHub)
        return serviceHub.signInitialTransaction(transactionBuilder)
    }

    @Suspendable
    private fun collectSignature(
            transaction: SignedTransaction,
            sessions: List<FlowSession>
    ): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, sessions))

    @Suspendable
    private fun recordRegistration(
            transaction: SignedTransaction,
            sessions: List<FlowSession>
    ): SignedTransaction = subFlow(FinalityFlow(transaction, sessions))

}

@InitiatedBy(KYCSpySettleFlow::class)
class SpySettle(private val session: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // receive the flag
        val needsToSignTransaction = session.receive<Boolean>().unwrap { it }
        // only sign if instructed to do so
        if (needsToSignTransaction) {
            subFlow(object : SignTransactionFlow(session) {
                override fun checkTransaction(stx: SignedTransaction) { }
            })
        }
        // always save the transaction
        return subFlow(ReceiveFinalityFlow(otherSideSession = session))
    }
}