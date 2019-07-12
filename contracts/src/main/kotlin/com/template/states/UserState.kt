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
        val sender: Party,
        val receiver: Party,
        val verify: Boolean = false,

        override val linearId: UniqueIdentifier = UniqueIdentifier(),
        override val participants: List<Party> = listOf(sender,receiver)
) : LinearState{

    @CordaSerializable
    data class Name(
            var firstName: String,
            var lastName: String,
            var age: String,
            var gender: String,
            var address: String
    )
}