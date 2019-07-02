package com.template.flows

import com.template.contracts.KYCContract
import com.template.flows.progressTracker.*
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
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
        return recordTransaction(signedTransaction, emptyList())
    }

    private fun transaction(): TransactionBuilder {
        progressTracker.currentStep = BUILDING
        val notary = firstNotary
        val refState = getKYCByLinearId(UniqueIdentifier.fromString(id))

        val states = serviceHub.vaultService
        val refStateData = refState.state.data
        val output = refStateData.verify()

        val verifyCommand = Command(KYCContract.Commands.Verify(), output.participants.map { it.owningKey })
        val builder = TransactionBuilder(notary = notary)
        builder.addInputState(refState)
        builder.addOutputState(output, KYCContract.KYC_CONTRACT_ID)
        builder.addCommand(verifyCommand)
        return builder
    }

}