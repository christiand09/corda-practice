package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import kotlin.math.absoluteValue

@InitiatingFlow
@StartableByRPC
class UserUpdateFlow (private var fullName: String,
                      private var Age: Int,
                      private var Gender: String,
                      private var Address: String,
                      private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction {

        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val inputState = serviceHub.vaultService.queryBy<UserState>(criteria).states.single()
        val input = inputState.state.data
        val receive = inputState.state.data.receiver
        val notary = inputState.state.notary

        if(fullName == "") fullName = input.newName(input.fullname).fullname
        if(Age != input.age) Age = input.newAge(input.age).age
        if(Gender == "") Gender = input.newGender(input.gender).gender
        if(Address == "") Address = input.newAddress(input.address).address
        val userState = UserState(fullName, Age, Gender, Address, input.sender, input.receiver, true,linearId = linearId)
        val cmd = Command(UserContract.Commands.Update(),ourIdentity.owningKey)
        val txBuilder = TransactionBuilder(notary = notary)
                .addInputState(inputState)
                .addOutputState(userState, UserContract.ID_Contracts)
                .addCommand(cmd)
        txBuilder.verify(serviceHub)
        val signedtx = serviceHub.signInitialTransaction(txBuilder)
        val session = initiateFlow(inputState.state.data.receiver)

        val ssss = subFlow(CollectSignaturesFlow(signedtx, listOf(session)))

        return subFlow(FinalityFlow(ssss, session))

    }
    @InitiatedBy(UserUpdateFlow::class)
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