package com.template.flows.heartBeat

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.SchedulableState
import net.corda.core.contracts.ScheduledActivity
import net.corda.core.contracts.StateRef
import net.corda.core.flows.FlowLogicRefFactory

import net.corda.core.identity.Party
import java.time.Instant



/**
 * Every Heartbeat state has a scheduled activity to start a flow to consume itself and produce a
 * new Heartbeat state on the ledger after five seconds.
 *
 * @property me The creator of the Heartbeat state.
 * @property nextActivityTime When the scheduled activity should be kicked off.
 */
@BelongsToContract(HeartBeatContract::class)
class HeartBeatState(private val me : Party,
                     private val nextActivityTime : Instant = Instant.now().plusSeconds(1)) : SchedulableState {

        override val participants get() = listOf(me)
        override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity?
        {
        return ScheduledActivity(flowLogicRefFactory.create(HeartBeatFlow::class.java, thisStateRef), nextActivityTime)
        }
}