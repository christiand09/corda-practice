package com.template.flows.tokenAmount

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.KYCContract
import com.template.states.KYCState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class KYCSelfIssueFlow(private val amount: Long, val linearId: UniqueIdentifier ):FlowLogic<SignedTransaction>(){

    @Suspendable
    override fun call(): SignedTransaction {
        val kycSelfIssueFlow = kycSelfIssueFlow()
        val signedTransaction = verifyAndSign(kycSelfIssueFlow)
        return recordTransaction (signedTransaction)
    }

    private fun inputStateRef(): StateAndRef<KYCState>{
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<KYCState>(criteria).states.single()
    }

    private fun outputState(): KYCState{
        val input = inputStateRef().state.data

        return KYCState(input.moneyLend,input.moneyBalance.plus(amount),input.requestedAmount,input.lender,input.borrower,"Deposit $amount into moneyBalance",input.linearId)
    }
    private fun kycSelfIssueFlow():TransactionBuilder{
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val kycCommand = Command (KYCContract.Commands.SelfIssue(), ourIdentity.owningKey)
        return TransactionBuilder(notary)
                .addInputState(inputStateRef())
                .addOutputState(outputState(),KYCContract.ID)
                .addCommand(kycCommand)
    }
    private fun verifyAndSign (transactionBuilder: TransactionBuilder): SignedTransaction {
        transactionBuilder.verify(serviceHub)
        return serviceHub.signInitialTransaction(transactionBuilder)
    }

    @Suspendable
    private fun recordTransaction(transaction: SignedTransaction): SignedTransaction =
            subFlow(FinalityFlow(transaction, emptyList()))


}