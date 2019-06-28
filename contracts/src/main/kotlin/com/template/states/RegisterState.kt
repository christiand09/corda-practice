package com.template.states


import com.template.contracts.RegisterContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import java.lang.Integer.parseInt

@BelongsToContract(RegisterContract::class)
data class RegisterState (val firstName: String,
                          val lastName: String,
                          val age: Int,
                          val gender: String,
                          val address: String,
                          val sender: Party,
                          val receiver: Party,
                          val approved: Boolean = false,
                          override val participants: List<Party> = listOf(sender, receiver),
                          override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState
{
    fun retainFirstName(oldFirstName: String) = copy(firstName = oldFirstName)
    fun retainLastName(oldLastName: String) = copy(lastName = oldLastName)
    fun retainAge(oldAge: String) = copy(age = parseInt(oldAge))
    fun retainGender(oldGender: String) = copy(gender = oldGender)
    fun retainAddress(oldAddress: String) = copy(address = oldAddress)
}
