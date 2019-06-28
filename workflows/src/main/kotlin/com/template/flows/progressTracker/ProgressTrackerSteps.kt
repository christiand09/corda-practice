package com.template.flows.progressTracker

import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

object INITIALIZING : Step("Performing initial steps.")
object BUILDING : Step("Building and verifying transaction.")
object SIGNING : Step("Signing transaction.")
object COLLECTING : Step("Collecting counterparty signature.") {
    override fun childProgressTracker() = CollectSignaturesFlow.tracker()
}
object FINALIZING : Step("Finalizing transaction.") {
    override fun childProgressTracker() = FinalityFlow.tracker()
}