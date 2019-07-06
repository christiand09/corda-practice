package com.template.states



import com.template.contracts.PersonalContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(PersonalContract::class)
data class PersonalState (
        val fullName: String,
        val age : Int,
        val birthDate : String,
        val address : String,
        val contactNumber: Int,
        val status : String,
        val send : Party,
        val receive: Party,
        val verify: Boolean,
        override val linearId: UniqueIdentifier = UniqueIdentifier(),
        override val participants: List<Party> = listOf(send, receive)
): LinearState