package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.UserState
import net.corda.core.contracts.Command
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

@InitiatingFlow
@StartableByRPC
class VerifyFlow (private val receiver: Party,
                  private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>(){


    override val progressTracker = ProgressTracker(
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            NOTARIZE_TRANSACTION,
            FINALISING_TRANSACTION )

    @Suspendable
    override fun call() : SignedTransaction {




//        progressTracker.currentStep = GENERATING_TRANSACTION
//        // verify notary
//
//
//        //Search in servicehub in the map all the parties listed and change the string in a Party
////        val OwnerRef = serviceHub.identityService.partiesFromName(OwnParty, false).singleOrNull()
////                ?: throw IllegalArgumentException("No match found for Owner $OwnParty.")
//
//        // Initiator flow logic goes here.
//        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
//        //get the information from KYCState
//        val Vault = serviceHub.vaultService.queryBy<UserState>(criteria).states.single()
//        val input = Vault.state.data
//        val receiver= Vault.state.data.receiver
//        val notary = Vault.state.notary
//        // belong to the transaction
//        val outputState = UserState(input.firstName,input.lastName,input.age,input.gender,input.address,input.sender,input.receiver,true  )
//
//
////        val state = UserState(input.firstName,input.lastName,input.age,input.gender,input.address,input.sender,input.receiver,true )
//        // valid or invalid in contract
//        val cmd = Command(UserContract.Commands.Verify(),listOf(receiver.owningKey, ourIdentity.owningKey))
//
//        //add transaction Builder
//        val txBuilder = TransactionBuilder(notary= notary)
//                .addInputState(Vault)
//                .addOutputState(outputState, REGISTER_ID)
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
//
//        progressTracker.currentStep = NOTARIZE_TRANSACTION
//        progressTracker.currentStep = FINALISING_TRANSACTION
//        //Notarize then Record the transaction
//        val stx = subFlow(CollectSignaturesFlow(signedTx, listOf(session)))
//        return subFlow(FinalityFlow(stx,session))


        progressTracker.currentStep = GENERATING_TRANSACTION
        val unverified = userVerified()

        progressTracker.currentStep = VERIFYING_TRANSACTION
        progressTracker.currentStep = SIGNING_TRANSACTION
        val signedTransaction = verifyAndSign(transaction = unverified)
        val sessions = (outputState().participants - ourIdentity).map { initiateFlow(it) }.toList()
        val allsignature = collectSignature(signedTransaction, sessions)
        progressTracker.currentStep = FINALISING_TRANSACTION

        return recordTransaction(transaction = allsignature, sessions = sessions)


    }

    private fun inputStateRef(): StateAndRef<UserState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
//        //get the information from KYCState
        return serviceHub.vaultService.queryBy<UserState>(criteria).states.single()


    }
    private fun outputState(): UserState{
        val input = inputStateRef().state.data
        return UserState(input.firstName,input.lastName,input.age,input.gender,input.address,sender = ourIdentity,receiver = receiver,verify = true,linearId = input.linearId)

    }

    private fun userVerified(): TransactionBuilder{
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val cmd = Command(UserContract.Commands.Register(), outputState().participants.map { it.owningKey })
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(outputState(),UserContract.REGISTER_ID)
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



    @InitiatedBy(VerifyFlow::class)
    class VerifyFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

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