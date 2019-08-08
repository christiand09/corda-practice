package schedulable

import com.fasterxml.jackson.databind.ObjectMapper
import net.corda.client.jackson.JacksonSupport
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@BelongsToContract(SampleContract::class)
@CordaSerializable
class SampleState(val owner: Party,
                  val amount: Long,
                  private val responseTime: Instant = Instant.now().plusSeconds(10),
                  override val linearId: UniqueIdentifier) : SchedulableState, LinearState
{
    override val participants: List<AbstractParty>
        get() = listOf(owner)

    override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity?
    {
        return ScheduledActivity(flowLogicRefFactory.create(
                SampleSelfIssueFlow::class.java, thisStateRef),
                responseTime
        )
    }
}