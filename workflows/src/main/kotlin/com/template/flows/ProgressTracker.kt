package com.template.flows

import net.corda.core.flows.FinalityFlow
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

object INITIALIZING : ProgressTracker.Step("Initializing transaction.")
object BUILDING : ProgressTracker.Step("Building transaction")
object SIGNING : ProgressTracker.Step("Signing transaction with our private key.")
object COLLECTING : ProgressTracker.Step("Collecting Signatures")
object FINALIZING : Step("Recording transaction.") {
    override fun childProgressTracker() = FinalityFlow.tracker()
}