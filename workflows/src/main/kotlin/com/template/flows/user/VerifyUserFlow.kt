package com.template.flows.user

import co.paralleluniverse.fibers.Suspendable
import com.google.common.collect.ImmutableList
import com.template.contracts.UserContract
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class VerifyUserFlow(private val id: String,
                     private val party: String) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val transaction = transaction()
        val signedTransaction = verifyAndSign(transaction)
        val sessions = (output().participants - ourIdentity).map { initiateFlow(it) }.toSet().toList()
        val transactionSignedByAllParties = collectSignature(signedTransaction, sessions)
        return recordTransaction(transactionSignedByAllParties, sessions)
    }

    private fun transaction(): TransactionBuilder {
        val notary: Party = serviceHub.networkMapCache.notaryIdentities.first()

        val refState = getKYCByLinearId(UniqueIdentifier.fromString(id))

        val verifyCommand =
                Command(UserContract.Commands.Verify(), output().participants.map { it.owningKey })
        val builder = TransactionBuilder(notary = notary)
        builder.addInputState(refState)
        builder.addOutputState(output(), UserContract.USER_CONTRACT_ID)
        builder.addCommand(verifyCommand)
        return builder
    }

    private fun output(): UserState {
        return getKYCByLinearId(UniqueIdentifier.fromString(id)).state.data.verify(listOf(ourIdentity, stringToParty(party)))
    }

    private fun stringToParty(party: String) : Party {
        return serviceHub.identityService.partiesFromName(party, false).single()
    }

    private fun getKYCByLinearId(linearId: UniqueIdentifier): StateAndRef<UserState> {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                null,
                ImmutableList.of(linearId),
                Vault.StateStatus.UNCONSUMED, null)

        return serviceHub.vaultService.queryBy<UserState>(queryCriteria).states.singleOrNull()
                ?: throw FlowException("User with id $linearId not found.")
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

@InitiatedBy(VerifyUserFlow::class)
class VerifyUserFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an user transaction" using (output is UserState)
            }
        }
        val signedTransaction = subFlow(signTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }
}