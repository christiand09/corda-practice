package com.template.flows.issflows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.LifePolicyContract
import com.template.flows.issfunctions.*
import com.template.states.LifePolicyState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class ApplyLoanFlow (private val counterParty: String,
                     private val linearId: UniqueIdentifier): IssFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val insurance = stringToParty("Insurance")

        progressTracker.currentStep = CREATING
        val applyLoanTransaction = applyLoan()

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(transaction = applyLoanTransaction)
        val sessions = initiateFlow(stringToParty(counterParty))
        val insuranceSession = initiateFlow(insurance)
        sessions.send(true) // Bank
        insuranceSession.send(false) // Insurance
        val transactionSignedByParties = collectSignature(transaction = signedTransaction, sessions = listOf(sessions))

        progressTracker.currentStep = NOTARIZING
        progressTracker.currentStep = FINALIZING
        return recordTransactionWithOtherParty(transaction = transactionSignedByParties, sessions = listOf(sessions, insuranceSession))
    }

    private fun outState(): LifePolicyState
    {
        val input = inputStateRef(linearId).state.data
        val insurance = stringToParty("Insurance")
        return LifePolicyState(
                user = input.user,
                bankParty = stringToParty(counterParty),
                insuranceParty = insurance,
                loan = true,
                linearId = linearId
        )
    }

    private fun applyLoan(): TransactionBuilder =
            TransactionBuilder(notary = inputStateRef(linearId).state.notary).apply {
                val applyLoanCommand =
                        Command(LifePolicyContract.Commands.ApplyLoan(),
                        listOf(ourIdentity.owningKey, stringToParty(counterParty).owningKey))
                val insurance = stringToParty("Insurance")
                val stateWithInsurance = outState().copy(participants = outState().participants + insurance)
                addInputState(inputStateRef(linearId))
                addOutputState(stateWithInsurance, LifePolicyContract.LIFE_ID)
                addCommand(applyLoanCommand)
            }
}

@InitiatedBy(ApplyLoanFlow::class)
class ApplyLoanFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val needsToSignTransaction = flowSession.receive<Boolean>().unwrap{ it }
        if (needsToSignTransaction)
        {
            subFlow(object : SignTransactionFlow(flowSession)
            {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be a loan transaction" using (output is LifePolicyState)
                }
            })
        }
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
    }
}