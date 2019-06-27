package com.template.flows

import net.corda.core.flows.FinalityFlow
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction.")
object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
object NOTARIZE_TRANSACTION : ProgressTracker.Step("Notarizing Transaction")
object FINALISING_TRANSACTION : Step("Recording transaction.") {
    override fun childProgressTracker() = FinalityFlow.tracker()
}