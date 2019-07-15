package com.template.contracts

import com.template.states.AttachmentState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction




class AttachmentContract : Contract {

    companion object {
        @JvmStatic
        // Used to identify our contract when building a transaction.
        val ATTACHMENT_ID = "com.template.contracts.AttachmentContract"
    }

    object Attach: TypeOnlyCommandData ()

    override fun verify(tx: LedgerTransaction) {
        val state = tx.outputsOfType<AttachmentState>().single()

        require(tx.attachments.any { it.id == state.attachId }) {
            "At least one attachment in transaction must match hash ${state.attachId}"
        }
    }



}

