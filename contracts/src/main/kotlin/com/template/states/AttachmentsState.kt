package com.template.states

import com.template.contracts.AttachmentsContract

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party



@BelongsToContract(AttachmentsContract::class)
data class AttachmentsState(
        val sender: Party,
        val receiver: Party,
        val attachId: SecureHash,
        override val linearId: UniqueIdentifier = UniqueIdentifier(),
        override val participants: List<Party> = listOf(sender,receiver)
) : LinearState