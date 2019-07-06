package com.template.states

import com.template.contracts.UserContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable


@BelongsToContract(UserContract::class)
data class UserState(
        val name: Name,
        val age: Int,
        val verified: Boolean = false,
        override val linearId: UniqueIdentifier = UniqueIdentifier(),
        override val participants: List<Party>
) : LinearState {

    fun verify(parties: List<Party>) = copy(verified = true, participants = parties)
    fun verify() = copy(verified = true)

}

@CordaSerializable
data class Name(
        val firstName: String,
        val lastName: String
)