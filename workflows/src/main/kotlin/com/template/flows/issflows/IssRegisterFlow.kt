package com.template.flows.issflows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.LifePolicyContract
import com.template.flows.issfunctions.*
import com.template.states.LifePolicyState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class IssRegisterFlow : IssFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val insurance = stringToParty("Insurance")

        progressTracker.currentStep = CREATING
        val registration = register(insurance)

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(transaction = registration)
        val sessions = emptyList<FlowSession>()
        val transactionSignedByParties = collectSignature(transaction = signedTransaction, sessions = sessions)

        progressTracker.currentStep = NOTARIZING
        progressTracker.currentStep = FINALIZING
        return recordTransactionWithOtherParty(transaction = transactionSignedByParties, sessions = sessions)
    }

    private fun outState(): LifePolicyState
    {
        return LifePolicyState(
                user = ourIdentity,
                bankParty = ourIdentity,
                insuranceParty = ourIdentity,
                loan = false
        )
    }

    private fun register(Insurance: Party): TransactionBuilder =
            TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first()).apply {
                val registerCommand = Command(LifePolicyContract.Commands.Register(), ourIdentity.owningKey)
                addOutputState(outState(), LifePolicyContract.LIFE_ID)
                addCommand(registerCommand)
            }
}

//@InitiatedBy(IssRegisterFlow::class)
//class IssRegisterFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
//{
//    @Suspendable
//    override fun call(): SignedTransaction
//    {
//        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
//    }
//}