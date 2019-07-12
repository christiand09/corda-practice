package com.template.flows.TokenFlow

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TokenContract
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import com.template.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.flows.*


abstract class TransactionFlows : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker(INITIALIZING, BUILDING, SIGNING, COLLECTING, FINALIZING, VERIFYING)

    fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

//    @Suspendable
//    fun collectSignature(
//            transaction: SignedTransaction,
//            sessions: List<FlowSession>
//    ): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, sessions))
//
//    @Suspendable
//    fun recordTransactionWithOtherParty(transaction: SignedTransaction, sessions: List<FlowSession>) : SignedTransaction {
//        progressTracker.currentStep = FINALIZING
//        return subFlow(FinalityFlow(transaction, sessions))
//    }
//
//    @Suspendable
//    fun recordTransactionWithoutOtherParty(transaction: SignedTransaction) : SignedTransaction {
//        progressTracker.currentStep = FINALIZING
//        return subFlow(FinalityFlow(transaction, emptyList()))
//    }
    fun stringToUniqueIdentifier(id: String): UniqueIdentifier {
        return UniqueIdentifier.fromString(id)
    }
    fun stringtoParty(counterparty:String): Party
    {
        return serviceHub.identityService.partiesFromName(counterparty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterparty.")
    }

    fun inputStateAndRefTokenState(id: String): StateAndRef<TokenState> {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(stringToUniqueIdentifier(id)))
        return serviceHub.vaultService.queryBy<TokenState>(queryCriteria).states.single()
    }
    fun transactionwithoutinput(admin:Party,state:TokenState,command: Command<TokenContract.Commands.Token>,notary:Party)
            = TransactionBuilder(notary = notary)
            .apply {
                val spiedOnMessage = state.copy(participants = state.participants + admin)
                addOutputState(spiedOnMessage, TokenContract.TOKEN_ID)
                addCommand(command)
            }
    fun transactionwithinput(admin:Party,state:TokenState,command: Command<TokenContract.Commands.Token>,linearId: String,notary: Party)
            = TransactionBuilder(notary = notary)
            .apply {
                val spiedOnMessage = state.copy(participants = state.participants + admin)
                addInputState(inputStateAndRefTokenState(linearId))
                addOutputState(spiedOnMessage, TokenContract.TOKEN_ID)
                addCommand(command)
            }
    fun fullypaidtransaction(admin: Party,state: TokenState,command: Command<TokenContract.Commands.Token>,linearId: String,notary:Party) = TransactionBuilder(notary = notary)
            .apply {
                val settle = state.copy(participants = listOf(admin))
                addInputState(inputStateAndRefTokenState(linearId))
                addOutputState(settle,TokenContract.TOKEN_ID)
                addCommand(command)
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

