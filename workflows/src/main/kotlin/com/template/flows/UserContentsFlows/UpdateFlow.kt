package com.template.flows.UserContentsFlows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.contracts.UserContract.Companion.REGISTER_ID
import com.template.flows.*
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import com.template.states.UserState.Name



@InitiatingFlow
@StartableByRPC
class UpdateFlow (var name: Name,var receiver: String,val linearId: UniqueIdentifier) : FlowFunctions() {

    override val progressTracker = ProgressTracker(
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            NOTARIZE_TRANSACTION,
            FINALISING_TRANSACTION)

    @Suspendable
    override fun call(): SignedTransaction {

        progressTracker.currentStep = GENERATING_TRANSACTION
        val unupdated = userUpdate()


        progressTracker.currentStep = VERIFYING_TRANSACTION
        progressTracker.currentStep = SIGNING_TRANSACTION
        val signedTransaction = verifyAndSign(transaction = unupdated)

        val counterRef = serviceHub.identityService.partiesFromName(receiver, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $receiver.")

        val sessions = initiateFlow(counterRef)
        val allsignature = collectSignature(signedTransaction, listOf(sessions))
        progressTracker.currentStep = FINALISING_TRANSACTION

        return recordTransaction(transaction = allsignature, sessions = listOf(sessions))

    }


    private fun outputState(): UserState{
        val input = inputStateRef(linearId).state.data

        val counterRef = stringToParty(receiver)

                    if(name.firstName=="")
                        name.firstName = input.name.firstName
                    if(name.lastName=="")
                        name.lastName = input.name.lastName
                    if(name.age=="")
                        name.age = input.name.age
                    if(name.gender=="")
                        name.gender = input.name.gender
                    if(name.address=="")
                        name.address = input.name.address

        return UserState(name,ourIdentity,counterRef,true,input.linearId)

    }
    private fun userUpdate(): TransactionBuilder{
        val counterRef = stringToParty(receiver)
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val cmd = Command(UserContract.Commands.Update(), listOf(ourIdentity.owningKey,counterRef.owningKey))
        val txBuilder = TransactionBuilder(notary)
                .addInputState(inputStateRef(linearId))
                .addOutputState(outputState(), REGISTER_ID)
                .addCommand(cmd)
        return txBuilder
    }

    @InitiatedBy(UpdateFlow::class)
    class UpdateFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

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