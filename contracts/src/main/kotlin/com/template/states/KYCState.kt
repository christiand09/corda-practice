package com.template.states

import com.template.contracts.KYCContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(KYCContract::class)
data class KYCState(
        val name: Name,
        val age: Int,
        val verified: Boolean,
        override val linearId: UniqueIdentifier,
        override val participants: List<AbstractParty>
) : LinearState{

    fun verify(parties: List<Party>) = copy(verified = true, participants = parties)
    fun addParticipants(participants: List<AbstractParty>) = copy(participants = participants)
}

@CordaSerializable
data class Name(
        val firstName: String,
        val lastName: String
)