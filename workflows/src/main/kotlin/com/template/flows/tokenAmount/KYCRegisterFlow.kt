package com.template.flows.tokenAmount

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.KYCContract
import com.template.contracts.KYCContract.Companion.ID
import com.template.states.KYCState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder


@InitiatingFlow
@StartableByRPC

class KYCRegisterFlow : FlowLogic<SignedTransaction>(){

    @Suspendable
    override fun call(): SignedTransaction {
        val kycRegister = kycRegister()
        val signedTransaction = verifyAndSign(kycRegister)
        return recordTransaction(signedTransaction)

    }

    private fun outputState(): KYCState {
        return KYCState(moneyLend = 0, moneyBalance = 0,requestedAmount = 0, lender = ourIdentity,borrower = ourIdentity,status = "", linearId = UniqueIdentifier())
    }

    private fun kycRegister(): TransactionBuilder {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val kycCommand = Command (KYCContract.Commands.Register(), outputState().participants.map { it.owningKey })
        return TransactionBuilder(notary)
                .addOutputState(outputState(),ID)
                .addCommand(kycCommand)
    }

    private fun verifyAndSign (transactionBuilder: TransactionBuilder) : SignedTransaction {
        transactionBuilder.verify(serviceHub)
        return serviceHub.signInitialTransaction(transactionBuilder)
    }


    @Suspendable

    private fun recordTransaction(transaction: SignedTransaction): SignedTransaction =
            subFlow(FinalityFlow(transaction, emptyList()))


}