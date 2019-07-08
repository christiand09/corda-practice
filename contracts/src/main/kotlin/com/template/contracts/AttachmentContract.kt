package com.template.contracts

import com.template.states.AttachmentState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction

class AttachmentContract : Contract
{
    companion object
    {
        @JvmStatic
        val ATTACHMENT_ID = "com.template.contracts.AttachmentContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val state = tx.outputsOfType<AttachmentState>().single()
    }
    interface Commands : CommandData {
        class Attachment :TypeOnlyCommandData(), Commands
    }
}
