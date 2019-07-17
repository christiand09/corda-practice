package com.template.flows.UserwithSchedulableFlow

import com.template.contracts.UserContract
import com.template.flows.TokenFlow.*
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

abstract class UserMethods: FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker(INITIALIZING, BUILDING, SIGNING, COLLECTING, FINALIZING, VERIFYING)

    fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    fun stringToUniqueIdentifier(id: String): UniqueIdentifier {
        return UniqueIdentifier.fromString(id)
    }

    fun inputStateAndRefTokenState(id: String): StateAndRef<UserState> {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(stringToUniqueIdentifier(id)))
        return serviceHub.vaultService.queryBy<UserState>(queryCriteria).states.single()
    }

    fun transactionwithoutinput(state: RegisterUserState, command: Command<RegisterUserContract.Commands.User>, notary: Party): TransactionBuilder {
        val builder = TransactionBuilder(notary = notary)
                .addCommand(command)
                .addOutputState(state = state, contract = UserContract.ID_Contracts)
        return builder
    }
}