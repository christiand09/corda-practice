package com.template.flows.schedulable

import com.template.flows.heartBeat.HeartBeatContract
import com.template.flows.heartBeat.HeartBeatFlow
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.SchedulableState
import net.corda.core.contracts.ScheduledActivity
import net.corda.core.contracts.StateRef
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.Party
import java.time.Instant

@BelongsToContract(schedulableContract::class)
class schedulableStatee( val delay: Long,
                         val count: Long,
                         val me : Party,
                         val startTime : Instant) : SchedulableState {

    override val participants get() = listOf(me)
    override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity?
    {
        val nextActivityTime : Instant = startTime.plusSeconds(delay)
        return ScheduledActivity(flowLogicRefFactory.create(schedulableSecondFlow::class.java, thisStateRef), nextActivityTime)
    }
}