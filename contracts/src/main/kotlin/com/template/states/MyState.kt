package com.template.states

import com.template.contracts.MyContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty

@BelongsToContract(MyContract::class)

data class MyState (val firstName: String,
                    val lastName: String,
                    val age: Int,
                    val gender: String,
                    val address: String,
                    val isApproved: Boolean,
                    override val participants: List<AbstractParty>):ContractState{}