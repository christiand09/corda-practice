package com.template.states

import com.template.contracts.UserContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty

@BelongsToContract(UserContract::class)
data class IOUState(
        val name: Name,
        val age: Int,
        val verified: Boolean,
        override val linearId: UniqueIdentifier,
        override val participants: List<AbstractParty>
        ) : LinearState 

data class Name(
        val firstName: String,
        val lastName: String
)