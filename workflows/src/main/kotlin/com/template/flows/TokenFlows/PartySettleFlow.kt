package com.template.flows.TokenFlows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TokenContract
import com.template.flows.*
import com.template.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap


@InitiatingFlow
@StartableByRPC
class PartySettleFlow(private val amountToPay: Long,
                      private val counterParty: String,
                      private val linearId: UniqueIdentifier) : FlowFunctions() {
    @Suspendable
    override fun call(): SignedTransaction {
        /*******************
         * SIGNEDTRANSACTION*
         *******************/
        if (outputState().settled) {
            progressTracker.currentStep = GENERATING_TRANSACTION
            val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
            val tx = verifyAndSign(transaction(spy))
            progressTracker.currentStep = VERIFYING_TRANSACTION
            progressTracker.currentStep = SIGNING_TRANSACTION
            val counterRef = stringToParty(counterParty)
            val sessions = initiateFlow(counterRef) // empty because the owner's signature is just needed
            val spySession = initiateFlow(spy)

            sessions.send(false)
            spySession.send(true)
            val transactionSignedByParties = collectSignature(transaction = tx, sessions = listOf(spySession))

            progressTracker.currentStep = FINALISING_TRANSACTION
            return recordTransaction(transaction = transactionSignedByParties, sessions = listOf(sessions, spySession))
        } else {
            progressTracker.currentStep = GENERATING_TRANSACTION
            val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
            val tx = verifyAndSign(transaction(spy))

            progressTracker.currentStep = VERIFYING_TRANSACTION
            progressTracker.currentStep = SIGNING_TRANSACTION

            val counterRef = stringToParty(counterParty)
            val sessions = initiateFlow(counterRef) // empty because the owner's signature is just needed
            val spySession = initiateFlow(spy)

            sessions.send(true)
            spySession.send(false)
            val transactionSignedByParties = collectSignature(transaction = tx, sessions = listOf(sessions))
            progressTracker.currentStep = FINALISING_TRANSACTION
            return recordTransaction(transaction = transactionSignedByParties, sessions = listOf(sessions, spySession))
        }

    }

    /*********************
     *SIGNEDTRANSACTIONEND*
     *********************/
    private fun outputState(): TokenState {
        val counterRef = stringToParty(counterParty)
        val input = inputStateRef(linearId).state.data
        val paid = input.amountPaid.plus(amountToPay)

        if (ourIdentity != input.borrower) {
            throw IllegalArgumentException("Amount settlement flow must be initiated by the borrower.")
        }
        if (input.walletBalance < 0){
            throw FlowException("You don't have balance to pay for your debt")
        }
        if (amountToPay > input.amountIssued){
            throw FlowException("Amount must be less than amount issued")
        }
        if (amountToPay > input.walletBalance){
            throw FlowException("There must be enough wallet amount before settling")
        }


        return if (paid == input.amountIssued) {
            TokenState(amountIssued = input.amountIssued,
                    amountPaid = paid,
                    borrower = input.borrower,
                    lender = counterRef,
                    iss = input.iss,
                    walletBalance = input.walletBalance.minus(amountToPay),
                    settled = true,
                    linearId = linearId)
        } else {
            TokenState(amountIssued = input.amountIssued,
                    amountPaid = paid,
                    borrower = input.borrower,
                    lender = counterRef,
                    iss = input.iss,
                    walletBalance = input.walletBalance.minus(amountToPay),
                    settled = false,
                    linearId = linearId)
        }
    }

    /****
     *SPY*
     ****/
    private fun transaction(spy: Party) =
            TransactionBuilder(notary = inputStateRef(linearId).state.notary).apply {
                val counterRef = stringToParty(counterParty)
                val spycmd =
                        if (outputState().settled == false) {
                            Command(TokenContract.Commands.Send(), listOf(ourIdentity.owningKey, counterRef.owningKey))
                        } else {
                            Command(TokenContract.Commands.Send(), listOf(spy.owningKey))
                        }

                // the spy is added to the messages participants
                if (!outputState().settled) {
                    val spiedOnMessage = outputState().copy(participants = outputState().participants + spy)
                    addInputState(inputStateRef(linearId))
                    addOutputState(spiedOnMessage, TokenContract.tokenID)
                    addCommand(spycmd)
                } else {
                      val spiedOnMessage = outputState().copy(participants = outputState().participants + spy - outputState().borrower - outputState().lender)
//                    val spiedOnMessage = outputState().copy(participants = listOf(spy))
                    addInputState(inputStateRef(linearId))
                    addOutputState(spiedOnMessage, TokenContract.tokenID)
                    addCommand(spycmd)
                }
            }
    /***/

}

@InitiatedBy(PartySettleFlow::class)
class PartySettleFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // receive the flag
        val needsToSignTransaction = flowSession.receive<Boolean>().unwrap { it }
        // only sign if instructed to do so
        if (needsToSignTransaction) {
            subFlow(object : SignTransactionFlow(flowSession) {
                override fun checkTransaction(stx: SignedTransaction) {}
            })
        }
        // always save the transaction
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
    }


}
