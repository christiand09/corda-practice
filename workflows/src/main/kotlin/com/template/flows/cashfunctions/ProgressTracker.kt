package com.template.flows.cashfunctions

import net.corda.core.flows.FinalityFlow
import net.corda.core.utilities.ProgressTracker

object CREATING : ProgressTracker.Step("Creating registration!")
object SIGNING : ProgressTracker.Step("Signing registration!")
object VERIFYING : ProgressTracker.Step("Verifying registration!")
object NOTARIZING : ProgressTracker.Step("Notarizing registration!")
object FINALIZING : ProgressTracker.Step("Finalize registration!")
{
    override fun childProgressTracker() = FinalityFlow.tracker()
}