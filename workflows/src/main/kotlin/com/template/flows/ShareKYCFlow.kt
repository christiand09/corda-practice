package com.template.flows

import com.template.contracts.UserContract
import com.template.flows.progressTracker.*
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class ShareKYCFlow(private val id : String) : UserBaseFlow() {

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
        val refStateData = refState.state.data
//
//        val inputCriteria = QueryCriteria.VaultQueryCriteria()
//        val states = serviceHub.vaultService.queryBy<KYC>

        check(!refStateData.verified){ throw FlowException("KYC with linearID: $id is not verified")}

        val output = refStateData.verify()

        val issueCommand = Command(UserContract.Commands.Share(), output.participants.map { it.owningKey })
        val builder = TransactionBuilder(notary = notary)
        builder.addOutputState(output, UserContract.USER_CONTRACT_ID)
        builder.addCommand(issueCommand)
        return builder
    }
}

