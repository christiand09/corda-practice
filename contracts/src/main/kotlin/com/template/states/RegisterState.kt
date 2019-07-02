package com.template.states


import com.template.contracts.RegisterContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(RegisterContract::class)
data class RegisterState (val name: Name,
                          val sender: Party,
                          val receivers: Party,
                          val approved: Boolean = false,
                          override val participants: List<Party> = listOf(sender, receivers),
                          override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState

@CordaSerializable
data class Name (var firstname: String,
                 var lastname: String,
                 var age: String,
                 var gender: String,
                 var address: String)

