package com.template.states

import com.template.contracts.MyContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@BelongsToContract (MyContract::class)
data class MyState (
        val registered: Registered ,
        val sender : Party,
        val receiver : Party,
        val verify : Boolean,
        override val linearId: UniqueIdentifier,
        override val participants : List <Party> = listOf(sender, receiver)

): LinearState

@CordaSerializable
data class Registered (
        var firstName : String,
        var lastName : String,
        var age : String,
        var birthDate: String,
        var address : String,
        var contactNumber : String,
        var status : String
)
