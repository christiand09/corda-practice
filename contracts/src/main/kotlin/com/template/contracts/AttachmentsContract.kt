
package com.template.contracts

import com.template.states.AttachmentsState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.hash
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction


class AttachmentsContract : Contract {

    override fun verify(tx: LedgerTransaction) {
//        val state = tx.outputsOfType<AttachmentsState>().single()
        // we check that at least one has the matching hash, the other will be the contract
//        require(tx.attachments.any { it.id == state.hash() }) {"At least one attachment in transaction must match hash ${state.hash()}"}

    }
    object Attach : TypeOnlyCommandData()
//    object UploadAttach: TypeOnlyCommandData()

//    data class State(val hash: SecureHash.SHA256) : ContractState {
//        override val participants: List<Party> = emptyList()
//    }
}
const val ATTACHMENT_PROGRAM_ID = "com.template.contracts.AttachmentsContract"