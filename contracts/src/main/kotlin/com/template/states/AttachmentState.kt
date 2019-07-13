package com.template.states

import com.template.contracts.AttachmentContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party

@BelongsToContract(AttachmentContract::class)
data class AttachmentState(val hash: SecureHash.SHA256,
                           val hash2: SecureHash.SHA256,
                           val hash3: SecureHash.SHA256,
                           val party: Party,
                           override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState
{
    override val participants: List<Party> = listOf(party)
}