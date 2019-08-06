package com.template.flows.myFunctionSample

import com.template.contracts.FunctionContract
import com.template.flows.FlowFunction
import com.template.states.FunctionState
import net.corda.core.contracts.Command
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

class RegisterFlowFunction(val name: String) : FlowFunction() {
    override fun call(): SignedTransaction {
        val signedTransaction = verifyAndSign(transactionBuilder())
        return recordTransactionWithOutParty(signedTransaction)
    }

    private fun outputState(): FunctionState {
        return FunctionState(name = name, status = false, sender = ourIdentity, receiver = ourIdentity)
    }

    private fun transactionBuilder(): TransactionBuilder {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val command = Command (FunctionContract.Commands.All(), ourIdentity.owningKey)
        return  TransactionBuilder(notary)
                .addOutputState(outputState(), FunctionContract.ID)
                .addCommand(command)
    }

}