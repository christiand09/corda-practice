package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.RegisterContract
import com.template.states.ClientInfo
import com.template.states.RegisterState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class RegisterFlow (private val clientInfo: ClientInfo) : FlowLogic<SignedTransaction>() {

    companion object {
        object BUILDING : ProgressTracker.Step ("Building Transaction")
        object VERIFYING : ProgressTracker.Step ("Verifying Transaction")
        object SIGNING : ProgressTracker.Step ("Signing Transaction")
        object FINALIZING : ProgressTracker.Step ("Finalizing Transaction")
    }

    private fun tracker() = ProgressTracker (BUILDING, VERIFYING, SIGNING, FINALIZING)

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // Initiator flow logic goes here.
        progressTracker.currentStep = BUILDING
        val registration = register(regState())

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction: SignedTransaction = verifyAndSign(registration)
        val session = emptyList<FlowSession>()
        val transactionSignedByAllParties = collectSignature(signedTransaction, session)
        progressTracker.currentStep = FINALIZING
        return recordRegistration(transactionSignedByAllParties, session)
    }
    private fun regState(): RegisterState {
        return RegisterState(clientInfo,ourIdentity,ourIdentity,false)
    }
    private fun register(regState: RegisterState): TransactionBuilder {
        val notary: Party = serviceHub.networkMapCache.notaryIdentities.first()
        val regCommand = Command(RegisterContract.Commands.Register(), ourIdentity.owningKey)
        val builder = TransactionBuilder(notary)
        builder.addOutputState(regState, RegisterContract.REGISTER_CONTRACT_ID)
        builder.addCommand(regCommand)
        return builder
    }
    private fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }
    @Suspendable
    private fun collectSignature (transaction: SignedTransaction, session: List<FlowSession>
    ): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, session))
    @Suspendable
    private fun recordRegistration(transaction: SignedTransaction,session: List<FlowSession>
    ): SignedTransaction = subFlow(FinalityFlow(transaction,session))
}

@InitiatedBy(RegisterFlow::class)
class Responder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        // Responder flow logic goes here.
        val signTransactionFlow = object: SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be Register" using (output is RegisterState)
            }
        }
        val txWeJustSignedId = subFlow(signTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))

    }
}
