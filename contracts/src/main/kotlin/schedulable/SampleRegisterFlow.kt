package schedulable

import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Instant

@StartableByRPC
class SampleRegisterFlow : SampleFunctions()
{
    override fun call(): SignedTransaction
    {
        val signedTransaction = verifyAndSign(register())
        return recordTransactionWithOtherParty(signedTransaction, listOf())
    }

    private fun outState(): SampleState
    {
        return SampleState(
                owner = ourIdentity,
                amount = 0,
                linearId = UniqueIdentifier()
        )
    }

    private fun register() = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first()).apply {
        addOutputState(outState(), SampleContract.contractId)
        addCommand(Command(SampleContract.Commands.Register(), ourIdentity.owningKey))
    }
}