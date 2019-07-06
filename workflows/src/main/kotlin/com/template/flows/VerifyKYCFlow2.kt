package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.KYCContract
import com.template.flows.progressTracker.*
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
class VerifyKYCFlow2(private val id : String,
                     private val party: String) : UserBaseFlow() {

    override val progressTracker = ProgressTracker(INITIALIZING, BUILDING, SIGNING, COLLECTING, FINALIZING)

    override fun call(): SignedTransaction {

        progressTracker.currentStep = INITIALIZING

        val transaction = transaction()
        val signedTransaction = verifyAndSign(transaction)
        val session = initiateFlow(stringToParty(party))
        val transactionSignedByAllParties = subFlow(CollectSignaturesFlow(signedTransaction, listOf(session)))
        return subFlow(FinalityFlow(transactionSignedByAllParties, listOf(session)))
    }

    private fun transaction(): TransactionBuilder {

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val verifyCommand =
                Command(KYCContract.Commands.Verify(), listOf(ourIdentity, stringToParty(party)).map { it.owningKey })
        val refState = getKYCByLinearId(UniqueIdentifier.fromString(id))

        val refStateData = refState.state.data
        val output = refStateData.verify(listOf(ourIdentity, stringToParty(party)))
        return TransactionBuilder(notary)
                .addInputState(refState)
                .addOutputState(output, KYCContract.KYC_CONTRACT_ID)
                .addCommand(verifyCommand)
    }

}

@InitiatedBy(VerifyKYCFlow2::class)
class VerifyKYC2FlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {

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