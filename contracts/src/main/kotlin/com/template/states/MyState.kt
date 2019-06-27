package com.template.states

import com.template.contracts.MyContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(MyContract::class)

data class MyState (var firstName: String,
                    val lastName: String,
                    val age: Int,
                    val gender: String,
                    val address: String,
                    val sender: Party,
                    val receiver: Party,
                    val approvals: Boolean,
                    override  val linearId: UniqueIdentifier = UniqueIdentifier()
                    ) : LinearState{
                    override val participants: List<Party> get() = listOf(sender, receiver)
    fun newFirstName(newFirstName: String) = copy(firstName = newFirstName)
                    }