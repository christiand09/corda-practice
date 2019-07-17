package com.template.flows.UserwithTimeWindow

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.flows.TokenFlow.*
import com.template.states.TokenState
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.time.Instant

abstract class Methods: FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker(INITIALIZING, BUILDING, SIGNING, COLLECTING, FINALIZING, VERIFYING)

    fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    @Suspendable
    fun collectSignature(
            transaction: SignedTransaction,
            sessions: List<FlowSession>): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, sessions))

    fun stringToUniqueIdentifier(id: String): UniqueIdentifier {
        return UniqueIdentifier.fromString(id)
    }

    fun inputStateAndRefTokenState(id: String): StateAndRef<UserState> {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(stringToUniqueIdentifier(id)))
        return serviceHub.vaultService.queryBy<UserState>(queryCriteria).states.single()
    }

    fun transactionwithoutinput(state: UserState, command: Command<UserContract.Commands.Register>, notary: Party): TransactionBuilder {
        val builder = TransactionBuilder(notary = notary)
                .addCommand(command)
                .addOutputState(state = state, contract = UserContract.ID_Contracts)
        return builder
    }
    fun getTime(linearId: String): Instant
    {
        val outputStateRef = StateRef(txhash = inputStateAndRefTokenState(linearId).ref.txhash, index = 0)
        val queryCriteria = QueryCriteria.VaultQueryCriteria(stateRefs = listOf(outputStateRef))
        val results = serviceHub.vaultService.queryBy<UserState>(queryCriteria)
        return results.statesMetadata.single().recordedTime
    }
    @Suspendable
    fun recordVerify(transaction: SignedTransaction,session: List<FlowSession>): SignedTransaction
            = subFlow(FinalityFlow(transaction,session))
}