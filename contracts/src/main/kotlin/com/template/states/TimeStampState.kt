package com.template.states

import com.template.contracts.TimeStampContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(TimeStampContract::class)
data class TimeStampState(
        val condition : Boolean,
        val sender : Party,
        val receiver: Party,
        override val linearId : UniqueIdentifier = UniqueIdentifier(),
        override val participants: List<AbstractParty> = listOf(sender, receiver)
):LinearState