package com.template.flows.TokenFlow

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TokenContract
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import com.template.states.TokenState
import net.corda.core.contracts.*
import net.corda.core.flows.*
import java.time.Duration
import java.time.Instant


abstract class TransactionFlows : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker(INITIALIZING, BUILDING, SIGNING, COLLECTING, FINALIZING, VERIFYING)

    fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    fun stringToUniqueIdentifier(id: String): UniqueIdentifier {
        return UniqueIdentifier.fromString(id)
    }

//    fun inputStateAndRefTokenState(id: String): StateAndRef<TokenState> {
//        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(stringToUniqueIdentifier(id)))
//        return serviceHub.vaultService.queryBy<TokenState>(queryCriteria).states.single()
//    }
    fun inputStateAndRefTokenState(id: UniqueIdentifier): StateAndRef<TokenState> {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(id))
        return serviceHub.vaultService.queryBy<TokenState>(queryCriteria).states.single()
    }
    fun transactionwithoutinput(admin:Party,state:TokenState,command: Command<TokenContract.Commands.Token>,notary:Party)
            = TransactionBuilder(notary = notary)
            .apply {
                val spiedOnMessage = state.copy(participants = state.participants + admin)
                addOutputState(spiedOnMessage, TokenContract.TOKEN_ID)
                addCommand(command)
            }

    fun transactionwithinput(admin:Party,state:TokenState, command: Command<TokenContract.Commands.Token>,
                             linearId: String, notary: Party) = TransactionBuilder(notary = notary)
    fun transactionwithinput(admin:Party,state:TokenState,command: Command<TokenContract.Commands.Token>,linearId: UniqueIdentifier,notary: Party)
            = TransactionBuilder(notary = notary)
            .apply {
                val spiedOnMessage = state.copy(participants = state.participants + admin)
                addInputState(inputStateAndRefTokenState(linearId))
                addOutputState(spiedOnMessage, TokenContract.TOKEN_ID)
                addCommand(command)
            }

    fun fullypaidtransaction(admin: Party,state: TokenState,command: Command<TokenContract.Commands.Token>,linearId: UniqueIdentifier,notary:Party) = TransactionBuilder(notary = notary)
            .apply {
                val settle = state.copy(participants = listOf(admin))
                addInputState(inputStateAndRefTokenState(linearId))
                addOutputState(settle,TokenContract.TOKEN_ID)
                addCommand(command)
            }
    fun getTime(linearId: UniqueIdentifier): Instant
    {
        val outputStateRef = StateRef(txhash = inputStateAndRefTokenState(linearId).ref.txhash, index = 0)
        val queryCriteria = QueryCriteria.VaultQueryCriteria(stateRefs = listOf(outputStateRef))
        val results = serviceHub.vaultService.queryBy<TokenState>(queryCriteria)
        return results.statesMetadata.single().recordedTime
    }
    @InitiatingFlow
    class CollectSignaturesInitiatingFlow(private val transaction: SignedTransaction,
                                          private val signers: List<Party>): FlowLogic<SignedTransaction>()
    {
        @Suspendable
        override fun call(): SignedTransaction {
            val sessions = signers.map{initiateFlow(it)}
            return subFlow(CollectSignaturesFlow(transaction,sessions))
        }
    }
    @InitiatedBy(CollectSignaturesInitiatingFlow::class)
    class CollectSignaturesResponder(private val session: FlowSession): FlowLogic<SignedTransaction>()
    {
        @Suspendable
        override fun call(): SignedTransaction {
            return subFlow(object : SignTransactionFlow(session) {
                override fun checkTransaction(stx: SignedTransaction) {
                }
            })
        }
    }
    fun stringToParty(name: String): Party {
        return serviceHub.identityService.partiesFromName(name, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for $name")
    }

}

object INITIALIZING : ProgressTracker.Step("Creating registration!")
object SIGNING : ProgressTracker.Step("Signing registration!")
object BUILDING : ProgressTracker.Step("Building registration!")
object COLLECTING : ProgressTracker.Step("Collecting registration!")
object FINALIZING : ProgressTracker.Step("Finalize registration!")
object VERIFYING : ProgressTracker.Step("Verifying registration!")

