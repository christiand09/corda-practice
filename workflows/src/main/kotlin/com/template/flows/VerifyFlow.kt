package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.RegisterContract
import com.template.states.RegisterState
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

@InitiatingFlow
@StartableByRPC
class VerifyFlow (private val counterParty: String,
                  private val linearId: UniqueIdentifier
                ): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val verification = verify()
        val signedTransaction = verifyAndSign(verification)
        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
        val session = initiateFlow(counterRef)
        val transactionSignedByAllParties = collectSignature(signedTransaction, listOf(session))
        return recordVerification(transactionSignedByAllParties, listOf(session))
    }

    private fun verify(): TransactionBuilder {
        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
        val notary = inputStateRef().state.notary
        val verifyCommand = Command(RegisterContract.Commands.Verify(), listOf(ourIdentity.owningKey, counterRef.owningKey))
        val builder = TransactionBuilder(notary)
        builder.addInputState(inputStateRef())
        builder.addOutputState(outputState(), RegisterContract.REGISTER_CONTRACT_ID)
        builder.addCommand(verifyCommand)
        return builder
        }

    private fun inputStateRef(): StateAndRef<RegisterState> {
        val ref = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<RegisterState>(ref).states.single()
    }

    private fun outputState(): RegisterState {
        val input = inputStateRef().state.data
        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
        return RegisterState(input.clientInfo,
                ourIdentity,
                counterRef,
                true,
                linearId = linearId)
    }

    private fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    @Suspendable
    private fun collectSignature (transaction: SignedTransaction, session: List<FlowSession>
    ): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, session))

    @Suspendable
    private fun recordVerification(transaction: SignedTransaction,session: List<FlowSession>
    ): SignedTransaction = subFlow(FinalityFlow(transaction,session))
}

@InitiatedBy(VerifyFlow::class)
class VerifyResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession)
        {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be Verify" using (output is RegisterState)
            }
        }
        val signedTransaction = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }
}