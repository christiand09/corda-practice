package com.template.states

import com.template.contracts.MyContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(MyContract::class)

data class MyState (var formSet : formSet,
                    var sender: Party,
                    val receiver: Party,
                    val approvals: Boolean =false,
                    override val participants: List<Party> = listOf(sender, receiver),
                    override  val linearId: UniqueIdentifier = UniqueIdentifier()
                    ) : LinearState

@CordaSerializable
data class formSet (
                    var firstName: String,
                    var lastName: String,
                    var age: String,
                    var gender: String,
                    var address: String)
