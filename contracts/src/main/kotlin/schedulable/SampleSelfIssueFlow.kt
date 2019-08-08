package schedulable

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateRef
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.SchedulableFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@SchedulableFlow
@StartableByRPC
class SampleSelfIssueFlow (private val sampleRef: StateRef): SampleFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val signedTransaction = serviceHub.signInitialTransaction(selfIssue())
        return subFlow(FinalityFlow(signedTransaction, listOf()))
    }

    private fun outState(): SampleState
    {
        val input = serviceHub.toStateAndRef<SampleState>(sampleRef).state.data
        return SampleState(
                owner = input.owner,
                amount = input.amount + 10,
                linearId = input.linearId
        )
    }

    private fun selfIssue() = TransactionBuilder(notary = serviceHub.toStateAndRef<SampleState>(sampleRef).state.notary).apply {
        val input = serviceHub.toStateAndRef<SampleState>(sampleRef)
        addInputState(input)
        addOutputState(outState(), SampleContract.contractId)
        addCommand(Command(SampleContract.Commands.SelfIssue(), ourIdentity.owningKey))
    }
}