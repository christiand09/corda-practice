package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.KYCContract
import com.template.flows.progressTracker.*
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class ShareKYCFlow(private val id : String,
                   private val parties: List<String>) : UserBaseFlow() {

    override val progressTracker = ProgressTracker(INITIALIZING, BUILDING, SIGNING, COLLECTING, FINALIZING)

    override fun call(): SignedTransaction {

        progressTracker.currentStep = INITIALIZING
        val transaction = transaction()
        val signedTransaction = verifyAndSign(transaction)
        val sessions = (stringToParty(parties) - ourIdentity).map { initiateFlow (it) }.toSet().toList()
        val transactionSignedByAllParties = collectSignature(signedTransaction, sessions)
        return recordTransaction(transactionSignedByAllParties, sessions)
    }

    private fun transaction(): TransactionBuilder {
        progressTracker.currentStep = BUILDING
        val notary = firstNotary
        val refState = getKYCByLinearId(UniqueIdentifier.fromString(id))
        val refStateData = refState.state.data

        check(!refStateData.verified){ throw FlowException("KYC with linearID: $id is not verified")}



        val output = refStateData.addParticipants(stringToParty(parties))
        val shareCommand = Command(KYCContract.Commands.Share(), stringToParty(parties).map { it.owningKey })
        val builder = TransactionBuilder(notary = notary)
        builder.addInputState(refState)
        builder.addOutputState(output, KYCContract.KYC_CONTRACT_ID)
        builder.addCommand(shareCommand)
        return builder
    }

    private fun stringToParty(parties : List<String>) : MutableList<Party>{
        val listOfParty = mutableListOf<Party>()
        for(party in parties) {
            listOfParty.add(serviceHub.identityService.partiesFromName(party, false).single())
        }
        return listOfParty
    }
}
@InitiatedBy(ShareKYCFlow::class)
class ShareKYCFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
//                "This must be an KYC transaction" using (output is KYCState)
            }
        }
        val signedTransaction = subFlow(signTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }
}

