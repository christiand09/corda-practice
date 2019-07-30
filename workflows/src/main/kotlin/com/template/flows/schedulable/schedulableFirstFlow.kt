package com.template.flows.schedulable

import co.paralleluniverse.fibers.Suspendable
import com.template.flows.*
import com.template.flows.heartBeat.HeartBeatContract
import com.template.flows.heartBeat.HeartBeatState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.time.Instant

@InitiatingFlow
@StartableByRPC
class schedulableFirstFlow(private val delay: Long) : FlowFunction() {
    override val progressTracker = ProgressTracker(INITIALIZING, BUILDING, SIGNING, COLLECTING, FINALIZING)

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = INITIALIZING
        val transaction: TransactionBuilder = transaction()
        val signedTransaction: SignedTransaction = verifyAndSign(transaction)
        return recordTransaction(signedTransaction, listOf())
    }

    private fun outputState(): schedulableStatee
    {
        return schedulableStatee(
                delay = delay,
                count = 0,
                me = ourIdentity,
                startTime = Instant.now()
        )
    }

    private fun transaction(): TransactionBuilder {
        progressTracker.currentStep = BUILDING
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val issueCommand = Command(schedulableContract.Commands.first(),ourIdentity.owningKey)
        val builder = TransactionBuilder(notary = notary )
        builder.addOutputState(outputState(), schedulableContract.contractID)
        builder.addCommand(issueCommand)
        return builder
    }
    }
