package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class UserRegisterFlow(private val fullname: String,
                       private val age:Int,
                       private val gender:String,
                       private val address: String,
                       private val receiver: Party):FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call():SignedTransaction
    {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val userState = UserState(fullname,age,gender,address,ourIdentity,receiver,false, listOf(ourIdentity), UniqueIdentifier())
        val cmd = Command(UserContract.Commands.Register(),ourIdentity.owningKey)
        val tbuilder = TransactionBuilder(notary)
                .addCommand(cmd)
                .addOutputState(userState, UserContract.ID_Contracts)
        val receive = userState.receiver
        tbuilder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(tbuilder)
        val name = userState.fullname.first().toString()
        if(name == fullname)
            throw IllegalArgumentException("Name already exists")
        val session = initiateFlow(receive)
        // Step 6. Collect the other party's signature using the SignTransactionFlow.
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(session)))

        // Step 7. Assuming no exceptions, we can now finalise the transaction.
        return subFlow(FinalityFlow(stx, session))
    }
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