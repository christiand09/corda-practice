package com.template.flows.UserwithTimeWindow

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Duration

@InitiatingFlow
@StartableByRPC
class Verify(private val linearId: String,private val receiver: Party): Methods() {

    @Suspendable
    override fun call(): SignedTransaction {
        val transaction: TransactionBuilder = verify()
        val signedTransaction = verifyAndSign(transaction)
        val session = initiateFlow(receiver)
        val transactionsignedbyboth = collectSignature(signedTransaction, sessions = listOf(session))
        return recordVerify(transaction = transactionsignedbyboth, session = listOf(session))
    }

    private fun verifyinput(): UserState {
        val inputstate = inputStateAndRefTokenState(linearId).state.data
        return UserState(inputstate.name, ourIdentity, receiver, true, linearId = stringToUniqueIdentifier(linearId))
    }
//    private fun verify():TransactionBuilder
//    {
//        val notary = inputStateAndRefTokenState(linearId).state.notary
//        val command = Command(UserContract.Commands.Verify(),
//                listOf(ourIdentity.owningKey,receiver.owningKey))
//        val builder = TransactionBuilder(notary = notary)
//        builder.addInputState(inputStateAndRefTokenState(linearId))
//        builder.addOutputState(state = verifyinput(),contract = UserContract.ID_Contracts)
//        builder.addCommand(command)
//        return builder
//    }
    private fun verify(): TransactionBuilder = TransactionBuilder(notary = inputStateAndRefTokenState(linearId).state.notary)
            .apply {
                val command = Command(UserContract.Commands.Verify(),
                        listOf(ourIdentity.owningKey, receiver.owningKey))
                val time = getTime(linearId)
                val timeWindow = TimeWindow.withTolerance(time, Duration.ofSeconds(30))
                        addCommand(command)
                        addInputState(inputStateAndRefTokenState(linearId))
                        addOutputState(state = verifyinput(), contract = UserContract.ID_Contracts)
                        setTimeWindow(timeWindow)
            }

    @InitiatedBy(Verify::class)
    class VerifyResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {

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