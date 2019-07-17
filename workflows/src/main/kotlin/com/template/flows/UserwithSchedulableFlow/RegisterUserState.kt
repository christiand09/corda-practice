package com.template.flows.UserwithSchedulableFlow

import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.sql.Time
import java.time.Instant

@BelongsToContract(RegisterUserContract::class)
class RegisterUserState(val initiator: Party,
                        val requestTime: Instant = Instant.now(),
                        val delay: Long = 10,
                        val linearId: UniqueIdentifier = UniqueIdentifier()): SchedulableState
{
    override val participants: List<Party> = listOf(initiator)
    override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity? {
        val responseTime = requestTime.plusSeconds(delay)
        val flowRef = flowLogicRefFactory.create(RegisterUserFlow::class.java)
        return ScheduledActivity(flowRef,responseTime)
    }
}