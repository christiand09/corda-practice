package com.template.contracts

import com.template.states.AttachmentState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction



class AttachmentContract : Contract
{
    companion object
    {
        @JvmStatic
        val ATTACHMENT_PROGRAM_ID = "com.template.contracts.AttachmentContract"
    }


    override fun verify(tx: LedgerTransaction) {
        val state = tx.outputsOfType<AttachmentState>().single()
        // we check that at least one has the matching hash, the other will be the contract
        // require(tx.attachments.any { it.id == state.hash }) {"At least one attachment in transaction must match hash ${state.hash}"}
    }

    object Attach : TypeOnlyCommandData()
}