package com.template.flows.user

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class VerifyAndShareToAll(private val id: String) : UserBaseFlow() {
    @Suspendable
    override fun call(): SignedTransaction {
        val transaction = transaction()
        val signedTransaction = verifyAndSign(transaction)
        val sessions = (output().participants - ourIdentity).map { initiateFlow(it) }.toSet().toList()
        val transactionSignedByAllParties = collectSignature(signedTransaction, sessions)
        return recordTransaction(transactionSignedByAllParties, sessions)
    }

    private fun transaction(): TransactionBuilder {
        val notary = firstNotary

        val refState = getUserByLinearId(UniqueIdentifier.fromString(id))

        val verifyCommand =
                Command(UserContract.Commands.Verify(), output().participants.map { it.owningKey })
        val builder = TransactionBuilder(notary = notary)
        builder.addInputState(refState)
        builder.addOutputState(output(), UserContract.USER_CONTRACT_ID)
        builder.addCommand(verifyCommand)
        return builder
    }

    private fun output(): UserState {
        return getUserByLinearId(UniqueIdentifier.fromString(id)).state.data.verify(listStringToParty(allParties()))
    }

}

@InitiatedBy(VerifyAndShareToAll::class)
class VerifyAndShareToAllResponder(val flowSession: FlowSession) : UserBaseFlow() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an user transaction" using (output is UserState)
            }
        }
        val signedTransaction = subFlow(signTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }
}