package com.template.states

import com.template.contracts.ClientContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(ClientContract::class)
data class ClientState(
        val calls : Calls,
        val sender: Party,
        val receiver: Party,
        val verify: Boolean,
        override val linearId: UniqueIdentifier = UniqueIdentifier(),
        override val participants: List<Party> = listOf(sender,receiver)
) : LinearState

@CordaSerializable
data class Calls(var name: String,
                 var age: String,
                 var address: String,
                 var birthDate: String,
                 var status: String,
                 var religion: String)
