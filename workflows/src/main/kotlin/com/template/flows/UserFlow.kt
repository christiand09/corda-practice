package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import sun.invoke.empty.Empty

@InitiatingFlow
@StartableByRPC


class UserFlow (

                 val firstName : String,
                 val lastName : String,
                 val age : Int,
                 val gender : String,
                 val address: String
                 ) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker(
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            NOTARIZE_TRANSACTION,
            FINALISING_TRANSACTION )

    @Suspendable
    override fun call() : SignedTransaction {
        progressTracker.currentStep = GENERATING_TRANSACTION
        val userRegister = userRegister(outputState())
        progressTracker.currentStep = VERIFYING_TRANSACTION
        progressTracker.currentStep = SIGNING_TRANSACTION
        val signedTransaction = verifyAndSign(transaction = userRegister)
        val sessions = emptyList<FlowSession>() // empty because the owner's signature is just needed
        val transactionSignedByParties = collectSignature(transaction = signedTransaction, sessions = sessions)


        progressTracker.currentStep = FINALISING_TRANSACTION
        return recordTransaction(transaction = transactionSignedByParties, sessions = sessions)
    }


    private fun outputState(): UserState{
        return UserState(firstName,lastName,age,gender,address,ourIdentity,ourIdentity)

    }
    private fun userRegister(state: UserState): TransactionBuilder{
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val cmd = Command(UserContract.Commands.Register(),ourIdentity.owningKey)
        val txBuilder = TransactionBuilder(notary)
               .addOutputState(state, UserContract.REGISTER_ID)
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

}
//        progressTracker.currentStep = GENERATING_TRANSACTION
//        // Initiator flow logic goes here.
//        // verify notary
//        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//
//        // belong to the transaction
//        val outputState = UserState(firstName,lastName,age,gender,address,sender,receiver,false)
//
//        // valid or invalid in contract
//        val cmd = Command(UserContract.Commands.Register(),ourIdentity.owningKey)
//
//        //add transaction Builder
//        val txBuilder = TransactionBuilder(notary)
//                .addOutputState(outputState, UserContract.REGISTER_ID)
//                .addCommand(cmd)
//
//        progressTracker.currentStep = VERIFYING_TRANSACTION
//        //verification of transaction
//        txBuilder.verify(serviceHub)
//
//        progressTracker.currentStep = SIGNING_TRANSACTION
//        //signed by the participants
//        val signedTx = serviceHub.signInitialTransaction(txBuilder)
//        val session= initiateFlow(receiver)
//
//        progressTracker.currentStep = FINALISING_TRANSACTION
//        //finalizing signature
//        return subFlow(FinalityFlow(signedTx,session))
//    }





    @InitiatedBy(UserFlow::class)
    class UserFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

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


