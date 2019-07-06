package com.template.flows

import com.template.contracts.KYCContract
import com.template.flows.progressTracker.*
import com.template.states.KYCState
import com.template.states.Name
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker


@InitiatingFlow
@StartableByRPC
class CreateKYCFlow(private val name: Name,
                    private val age: Int) : UserBaseFlow() {

    override val progressTracker = ProgressTracker(INITIALIZING, BUILDING,SIGNING, COLLECTING, FINALIZING)

    override fun call(): SignedTransaction {

        progressTracker.currentStep = INITIALIZING
        val transaction = transaction()
        val signedTransaction = verifyAndSign(transaction)
        return recordTransaction(signedTransaction, emptyList())
    }

    private fun transaction(): TransactionBuilder {
        progressTracker.currentStep = BUILDING
        val notary = firstNotary

        val output = KYCState(name, age, false, UniqueIdentifier(), listOf(ourIdentity))

        val issueCommand =  Command(KYCContract.Commands.Create(), output.participants.map { it.owningKey })
        val builder = TransactionBuilder(notary = notary)
        builder.addOutputState(output, KYCContract.KYC_CONTRACT_ID)
        builder.addCommand(issueCommand)
        return builder
    }

}