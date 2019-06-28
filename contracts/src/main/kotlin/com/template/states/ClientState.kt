package com.template.states

import com.template.contracts.ClientContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party


@BelongsToContract(ClientContract::class)
data class ClientState(
        val name: String,
        val age: Int,
        val receiver: Party,
        val sender: Party,
        val verify: Boolean,
        override val linearId: UniqueIdentifier = UniqueIdentifier(),
        override val participants: List<Party> = listOf(sender,receiver)
) : LinearState