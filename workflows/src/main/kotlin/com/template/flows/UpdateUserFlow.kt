package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.google.common.base.Strings.isNullOrEmpty
import com.template.contracts.MyContract
import com.template.states.MyState
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
class UpdateUserFlow( private var firstName: String,
                      private var lastName: String,
                      private var age: String ,
                      private var gender: String,
                      private var address: String,
                     private val receiver: Party,
                      private val linearId: UniqueIdentifier = UniqueIdentifier()
                    ): FlowLogic<SignedTransaction>(){

    /* Declare Transaction steps*/
    override val progressTracker = ProgressTracker(
            GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            VERIFYING_TRANSACTION,
            NOTARIZE_TRANSACTION,
            FINALISING_TRANSACTION
    )

    @Suspendable
    override fun call():SignedTransaction {
        val transaction: TransactionBuilder = transaction()
        val signedTransaction: SignedTransaction = verifyAndSign(transaction)
        val sessions = (outputState().participants - ourIdentity).map { initiateFlow(it) }.toList()
        val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, sessions)
        return recordTransaction(transactionSignedByAllParties, sessions)
    }

    private fun inputStateRef(): StateAndRef<MyState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<MyState>(criteria).states.single()
    }

    private fun outputState(): MyState{
        val input = inputStateRef().state.data
        return MyState(firstName,lastName,age,gender,address,ourIdentity,input.receiver,input.approvals, input.participants, input.linearId)
    }

    private fun transaction(): TransactionBuilder {
//        val inputCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
//        val vault = serviceHub.vaultService.queryBy<MyState>(inputCriteria).states.first()
//        var input = vault.state.data
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//        val userState = MyState(firstName, lastName, age, gender, address, ourIdentity, input.receiver, input.approvals, input.participants, input.linearId)

        when {
            outputState().firstName == "" -> outputState().firstName = inputStateRef().state.data.firstName
        }
        when {
            outputState().lastName == "" -> outputState().lastName = inputStateRef().state.data.lastName
        }
        when {
            outputState().gender == "" -> outputState().gender = inputStateRef().state.data.gender
        }
        when {
            outputState().address == "" -> outputState().address = inputStateRef().state.data.address
        }
        when {
            outputState().age == "" -> outputState().age = inputStateRef().state.data.age
        }

        val cmd = Command(MyContract.Commands.Issue(),ourIdentity.owningKey)
        val builder = TransactionBuilder(notary)
                .addInputState(inputStateRef())
                .addOutputState(outputState(),MyContract.IOU_CONTRACT_ID)
                .addCommand(cmd)
        progressTracker.currentStep = GENERATING_TRANSACTION
        return builder
    }

    private fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        transaction.verify(serviceHub)
        progressTracker.currentStep = VERIFYING_TRANSACTION
        progressTracker.currentStep = SIGNING_TRANSACTION
        progressTracker.currentStep = NOTARIZE_TRANSACTION
        progressTracker.currentStep = FINALISING_TRANSACTION
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





@InitiatedBy(UpdateUserFlow::class)
class UpdateUserFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction" using (output is MyState)
            }
        }
        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}