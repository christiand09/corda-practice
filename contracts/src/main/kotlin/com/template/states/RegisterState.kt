package com.template.states

import com.template.contracts.RegisterContract
import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

// *********
// * State *
// *********
@BelongsToContract(RegisterContract::class)
data class RegisterState(val clientInfo: ClientInfo,
                         val sender: Party,
                         val receiver: Party,
                         val verify: Boolean,
                         override val linearId: UniqueIdentifier = UniqueIdentifier(),
                         override val participants: List<Party> = listOf(sender, receiver)
             ) : LinearState

@CordaSerializable
data class ClientInfo (var firstName: String,
                     var lastName: String,
                     var age: String,
                     var gender: String,
                     var address: String)



