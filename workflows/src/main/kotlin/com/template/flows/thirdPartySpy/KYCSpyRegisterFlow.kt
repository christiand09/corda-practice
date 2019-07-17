package com.template.flows.thirdPartySpy

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.KYCSpyContract
import com.template.states.KYCSpyState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC

class KYCSpyRegisterFlow : FlowLogic<SignedTransaction>(){

    @Suspendable
    override fun call(): SignedTransaction {
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        val signedTransaction = verifyAndSign(transaction(spy))
        val spySession = initiateFlow(spy)
        val transactionSignedByAllParties = collectSignature(signedTransaction)

        return recordRegistration(transactionSignedByAllParties, listOf(spySession))
    }


    private fun outputState(): KYCSpyState {
        val spy = serviceHub.identityService.partiesFromName("PartyC", false).first()
        return KYCSpyState(moneyLend = 0, moneyBalance = 0,requestedAmount = 0, lender = ourIdentity,spy = spy,borrower = ourIdentity,status = "", linearId = UniqueIdentifier())
    }

    private fun transaction(spy: Party) =
            TransactionBuilder(notary= serviceHub.networkMapCache.notaryIdentities.first()).apply {
                // the spy is added to the messages participants
                val spiedOnMessage = outputState().copy(participants = outputState().participants + spy)
                addOutputState(spiedOnMessage, KYCSpyContract.ID)
                addCommand(Command(KYCSpyContract.Commands.Register(), ourIdentity.owningKey))
            }

    private fun verifyAndSign (transactionBuilder: TransactionBuilder) : SignedTransaction {
        transactionBuilder.verify(serviceHub)
        return serviceHub.signInitialTransaction(transactionBuilder)
    }

    @Suspendable
    private fun collectSignature(
            transaction: SignedTransaction
    ): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, emptyList()))

    @Suspendable
    private fun recordRegistration(
            transaction: SignedTransaction,
            sessions: List<FlowSession>
    ): SignedTransaction = subFlow(FinalityFlow(transaction, sessions))


}

@InitiatedBy(KYCSpyRegisterFlow::class)
class SpyRegister(private val session: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        return subFlow(ReceiveFinalityFlow(otherSideSession = session))
    }
}

