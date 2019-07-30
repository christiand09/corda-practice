package com.template.flows.schedulable

import co.paralleluniverse.fibers.Suspendable
import com.template.flows.*
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.time.Instant

@InitiatingFlow
@SchedulableFlow
class schedulableSecondFlow(private val stateRef: StateRef) : FlowLogic<Long>() {
    override val progressTracker = ProgressTracker(INITIALIZING, BUILDING, SIGNING, COLLECTING, FINALIZING)

    @Suspendable
    override fun call(): Long {
        progressTracker.currentStep = INITIALIZING
        val transaction: TransactionBuilder = transaction()
        val signedTransaction: SignedTransaction = verifyAndSign(transaction)
        recordTransaction(signedTransaction, listOf())
        val input = serviceHub.toStateAndRef<schedulableStatee>(stateRef).state.data
        return input.count + input.delay
    }

    private fun outputState(): schedulableStatee
    { val input = serviceHub.toStateAndRef<schedulableStatee>(stateRef).state.data
        return schedulableStatee(
                delay = input.delay,
                count = input.count+ input.delay ,
                me = ourIdentity,
                startTime = Instant.now()
        )
    }

    private fun transaction(): TransactionBuilder {
        progressTracker.currentStep = BUILDING
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val input = serviceHub.toStateAndRef<schedulableStatee>(stateRef)
        val issueCommand = Command(schedulableContract.Commands.first(),ourIdentity.owningKey)
        val builder = TransactionBuilder(notary = notary )
        builder.addInputState(input)
        builder.addOutputState(outputState(), schedulableContract.contractID)
        builder.addCommand(issueCommand)
        return builder
    }
    fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        progressTracker.currentStep = SIGNING
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }
    @Suspendable
    fun recordTransaction(transaction: SignedTransaction, sessions: List<FlowSession>): SignedTransaction {
        progressTracker.currentStep = FINALIZING
        return subFlow(FinalityFlow(transaction, sessions))
    }
}
