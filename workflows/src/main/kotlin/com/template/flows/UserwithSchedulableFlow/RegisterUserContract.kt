package com.template.flows.UserwithSchedulableFlow

import com.template.states.AttachmentState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction

class RegisterUserContract : Contract
{
    companion object
    {
        @JvmStatic
        val CONTRACT_ID = "com.template.contracts.AttachmentContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val state = tx.outputsOfType<RegisterUserState>().single()
    }
    interface Commands : CommandData {
        class User :TypeOnlyCommandData(), Commands
    }
}
