package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.contracts.UserContract.Companion.REGISTER_ID
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import org.hibernate.Transaction


@InitiatingFlow
@StartableByRPC
class UpdateFlow (

        var firstName : String,
        var lastName : String,
        var age : String,
        var gender : String,
        var address: String,
        var receiver: Party,
        val linearId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker(
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            NOTARIZE_TRANSACTION,
            FINALISING_TRANSACTION )

    @Suspendable
    override fun call(): SignedTransaction {
//        progressTracker.currentStep = GENERATING_TRANSACTION
//        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//
//        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
//        //get the information from KYCState
//        val Vault = serviceHub.vaultService.queryBy<UserState>(criteria).states.first()
//        val input = Vault.state.data
//        val receiver=Vault.state.data.receiver
//
//
//        if(firstName=="")
//            firstName = input.firstName
//        if(lastName=="")
//            lastName = input.lastName
//        if(age=="")
//            age = input.age.toString()
//        if(gender=="")
//            gender = input.gender
//        if(address=="")
//            address = input.address
//
//
//        val outputState = UserState(firstName,lastName,age.toInt(),address,input.gender,input.sender,input.receiver,input.verify,input.linearId)
//
//        val cmd = Command(UserContract.Commands.Verify(),listOf(receiver.owningKey, ourIdentity.owningKey))
//
//        val txBuilder = TransactionBuilder(notary)
//                .addInputState(Vault)
//                .addOutputState(outputState, REGISTER_ID)
//                .addCommand(cmd)
//
//        progressTracker.currentStep = VERIFYING_TRANSACTION
//        txBuilder.verify(serviceHub)
//
//        progressTracker.currentStep = SIGNING_TRANSACTION
//        val signedTx = serviceHub.signInitialTransaction(txBuilder)
//        val session= initiateFlow(receiver)
//
//
//        progressTracker.currentStep = NOTARIZE_TRANSACTION
//        progressTracker.currentStep = FINALISING_TRANSACTION
//        //Notarize then Record the transaction
//        val stx = subFlow(CollectSignaturesFlow(signedTx, listOf(session)))
//        return subFlow(FinalityFlow(stx,session))

        progressTracker.currentStep = GENERATING_TRANSACTION
        val unupdated = userUpdate()


        progressTracker.currentStep = VERIFYING_TRANSACTION
        progressTracker.currentStep = SIGNING_TRANSACTION
        val signedTransaction = verifyAndSign(transaction = unupdated)
        val sessions = (outputState().participants - ourIdentity).map { initiateFlow(it) }.toSet().toList()
        val allsignature = collectSignature(signedTransaction, sessions)
        progressTracker.currentStep = FINALISING_TRANSACTION



        return recordTransaction(transaction = allsignature, sessions = sessions)

    }



    private fun inputStateRef(): StateAndRef<UserState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))

        return serviceHub.vaultService.queryBy<UserState>(criteria).states.first()


    }


    private fun outputState(): UserState{
        val input = inputStateRef().state.data

        if(firstName=="")
            firstName = input.firstName
        if(lastName=="")
            lastName = input.lastName
        if(age=="")
            age = input.age.toString()
        if(gender=="")
            gender = input.gender
        if(address=="")
            address = input.address

        return UserState(firstName,lastName,age.toInt(),gender,address,ourIdentity,receiver,true,input.linearId)

    }

    private fun userUpdate(): TransactionBuilder{
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val cmd = Command(UserContract.Commands.Update(), listOf(ourIdentity.owningKey,receiver.owningKey))
        val txBuilder = TransactionBuilder(notary)
                .addInputState(inputStateRef())
                .addOutputState(outputState(), REGISTER_ID)
                .addCommand(cmd)
        return txBuilder
    }

    private fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        transaction.verify(serviceHub)

        return serviceHub.signInitialTransaction(transaction)
    }


    @Suspendable
    private fun collectSignature(
            transaction: SignedTransaction,
            sessions: List<FlowSession>
    ): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, sessions))


    @Suspendable
    private fun recordTransaction(transaction: SignedTransaction, sessions: List<FlowSession>): SignedTransaction =
            subFlow(FinalityFlow(transaction, sessions))


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