package com.template.states

import com.template.contracts.LifePolicyContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(LifePolicyContract::class)
data class LifePolicyState (
        val user: Party,
        val bankParty: Party,
        val insuranceParty: Party,
        val loan: Boolean,
        override val linearId: UniqueIdentifier = UniqueIdentifier(),
        override val participants: List<Party> = listOf(user,bankParty)) : LinearState
