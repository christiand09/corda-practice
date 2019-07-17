package com.template.flows.tokenAmount

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.KYCContract
import com.template.states.KYCState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder


@InitiatingFlow
@StartableByRPC
class KYCSettleFlow (private val amountPay: Long
                     , private val lender: String
                     , val linearId: UniqueIdentifier): FlowLogic<SignedTransaction>(){


    @Suspendable
    override fun call(): SignedTransaction {
       val kycSettleFlow = kycSettleFlow()
       val signedTransaction = verifyAndSign(kycSettleFlow)
        val counterRef = serviceHub.identityService.partiesFromName(lender, false).singleOrNull()
                ?:throw IllegalArgumentException("No match found for Owner $lender.")
        val sessions = initiateFlow(counterRef)
        val transactionSignedByAllParties = collectSignature(signedTransaction, listOf(sessions))

        if (ourIdentity != inputStateRef().state.data.borrower) {
            throw IllegalArgumentException("KYC settlement flow must be initiated by the borrower.")
        }

        return recordRegistration(transactionSignedByAllParties, listOf(sessions))
    }



    private fun inputStateRef() : StateAndRef<KYCState>{

        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<KYCState>(criteria).states.single()

    }

    private fun outputState() : KYCState {
        val input = inputStateRef().state.data
        val counterRef = serviceHub.identityService.partiesFromName(lender, false).singleOrNull()
                ?:throw IllegalArgumentException("No match found for Owner $lender.")


        if(amountPay > input.moneyLend) throw FlowException("You only owed ${input.moneyLend} but gives $amountPay which is more than the amount owed")
        return if(amountPay  < input.moneyLend)
        {
            KYCState(moneyLend = input.moneyLend.minus(amountPay), moneyBalance = input.moneyBalance.minus(amountPay),requestedAmount = input.requestedAmount,
                    lender = counterRef,borrower = ourIdentity, status = "PartialPaid",linearId = input.linearId)
        }
        else {
            KYCState(moneyLend = input.moneyLend.minus(amountPay), moneyBalance = input.moneyBalance.minus(amountPay),requestedAmount = input.requestedAmount,
                    lender = counterRef,borrower = ourIdentity, status = "Paid",linearId = input.linearId)

        }



    }

    private fun kycSettleFlow(): TransactionBuilder{
        val notary = inputStateRef().state.notary
        val settleCommand =
                Command(KYCContract.Commands.Settle(), outputState().participants.map {it.owningKey})
        return TransactionBuilder(notary)
                .addInputState(inputStateRef())
                .addOutputState(outputState(),KYCContract.ID)
                .addCommand(settleCommand)
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

@InitiatedBy(KYCSettleFlow::class)
class FlowSettle(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction
    {
        val signTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an issue transaction" using (output is KYCState)
            }
        }
        val signedTransaction = subFlow(signTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }
}
