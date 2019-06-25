package com.template.states


import com.template.contracts.RegisterContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(RegisterContract::class)
data class RegisterState (val sender: Party,
                          val firstName: String,
                          val lastName: String,
                          val age: Int,
                          val gender: String,
                          val address: String,
//                          val isApproved: Boolean = false,
                          val receiver: Party,
                          override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState
{
    override val participants: List<Party>
        get() = listOf(sender, receiver)
}
