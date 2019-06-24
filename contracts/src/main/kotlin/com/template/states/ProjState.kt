package com.template.states


import com.template.contracts.ProjContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(ProjContract::class)
data class ProjState(
        val firstName: String,
        val lastName: String,
        val age: Int,
        val gender: String,
        val address: String,
        val isApproved: Boolean,
        val unRegistered: Party,
        val toRegister: Party,
        override val linearId: UniqueIdentifier = UniqueIdentifier(),
        override val participants: List<Party> = listOf(unRegistered,toRegister)
) : LinearState