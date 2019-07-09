package com.template.states

import com.template.contracts.AttachmentContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable


@BelongsToContract(AttachmentContract::class)
data class AttachmentState ( val hashhh: SecureHash.SHA256,
                             val sender: Party,
                             val receiver: Party,
                            override val linearId: UniqueIdentifier = UniqueIdentifier()
                            ) : LinearState {
override val participants: List<Party> = listOf(sender, receiver)
}