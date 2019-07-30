package com.template.states



import com.template.contracts.MyContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(MyContract::class)
data class TimeWindowState(val status: Boolean,
                           val sender: Party,
                           val receiver: Party,
                           val spy: Party,
                           override val participants: List<Party> = listOf(sender, receiver),
                           override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState