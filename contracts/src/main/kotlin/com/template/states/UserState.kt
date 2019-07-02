package com.template.states

import com.template.contracts.UserContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(UserContract::class)
data class UserState(val fullname: String,
                     val age: Int,
                     val gender: String,
                     val address: String,
                     val sender: Party,
                     val receiver: Party,
                     val verify: Boolean,
                     override val participants: List<Party> = listOf(sender, receiver),
                     override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState
{

}


