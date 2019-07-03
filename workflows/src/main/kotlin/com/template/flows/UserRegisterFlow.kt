package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import com.template.states.UserDetails

@InitiatingFlow
@StartableByRPC
class UserRegisterFlow(private val name: UserDetails):FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call():SignedTransaction
    {
        val transaction: TransactionBuilder = transaction(registerstate())
        val signedTransaction:SignedTransaction = verifyandSign(transaction)
        val session:List<FlowSession> = emptyList()
        val transactionsSignedBothbyParties: SignedTransaction = collectSignatures(signedTransaction,session)
        return recordRegister(transaction = transactionsSignedBothbyParties,session = session)
    }
    private fun registerstate(): UserState
    {
        return UserState(name,ourIdentity,ourIdentity)
    }
    private fun transaction(state: UserState): TransactionBuilder
    {
        val notary : Party = serviceHub.networkMapCache.notaryIdentities.first()
        val issueCommand=
                Command(UserContract.Commands.Register(), ourIdentity.owningKey)
        val builder = TransactionBuilder(notary = notary)
                .addCommand(issueCommand)
                .addOutputState(state = state, contract = UserContract.ID_Contracts)
        return builder
    }
    private fun verifyandSign(transaction: TransactionBuilder): SignedTransaction
    {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }
    @Suspendable
    private fun collectSignatures(transaction: SignedTransaction,session: List<FlowSession>): SignedTransaction =
            subFlow(CollectSignaturesFlow(transaction,session))
    @Suspendable
    private fun recordRegister(transaction: SignedTransaction,session: List<FlowSession>): SignedTransaction =
            subFlow(FinalityFlow(transaction,session))

    @InitiatedBy(UserRegisterFlow::class)
    class IOUIssueFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an IOU transaction" using (output is UserState)
                }
            }

            val txWeJustSignedId = subFlow(signedTransactionFlow)

            return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
        }
    }
}