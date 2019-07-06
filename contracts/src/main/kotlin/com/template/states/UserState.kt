package com.template.states

import com.template.contracts.UserContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty


@BelongsToContract(UserContract::class)
data class UserState(
        val name: Name,
        val age: Int,
        val verified: Boolean = false,
        override val linearId: UniqueIdentifier = UniqueIdentifier(),
        override val participants: List<AbstractParty>
) : LinearState {

    fun verify(parties: List<AbstractParty>) = copy(verified = true, participants = parties)

}

//@CordaSerializable
//data class Name(
//        val firstName: String,
//        val lastName: String
//)