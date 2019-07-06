package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.KYCContract
import progressTracker.*
import com.template.states.KYCState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class VerifyKYCFlow(private val id : String) : UserBaseFlow() {

    override val progressTracker = ProgressTracker(INITIALIZING, BUILDING, SIGNING, COLLECTING, FINALIZING)

    override fun call(): SignedTransaction {

        progressTracker.currentStep = INITIALIZING
        val transaction = transaction()
        val signedTransaction = verifyAndSign(transaction)
        val sessions = (listStringToParty(allParties()) - ourIdentity).map{ initiateFlow(it)}
        val transactionSignedByAllParties = collectSignature(signedTransaction, sessions)
        return recordTransaction(transactionSignedByAllParties, sessions)
    }

    private fun transaction(): TransactionBuilder {
        progressTracker.currentStep = BUILDING
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val refState = getKYCByLinearId(UniqueIdentifier.fromString(id))

        val refStateData = refState.state.data
        val output = refStateData.verify(listStringToParty(allParties()))
        val verifyCommand = Command(KYCContract.Commands.Verify(), output.participants.map { it.owningKey })
        val builder = TransactionBuilder(notary = notary)
        builder.addInputState(refState)
        builder.addOutputState(output, KYCContract.KYC_CONTRACT_ID)
        builder.addCommand(verifyCommand)
        return builder
    }

}

@InitiatedBy(VerifyKYCFlow::class)
class VerifyKYCFlowResponder(val flowSession: FlowSession) : UserBaseFlow() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an KYC transaction" using (output is KYCState)
            }
        }
        val signedTransaction = subFlow(signTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }
}