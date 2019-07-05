package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.RegisterContract
import com.template.states.ClientInfo
import com.template.states.RegisterState
import net.corda.core.concurrent.firstOf
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class UpdateRegisterFlow (private var clientInfo: ClientInfo,
                  private val counterParty: String,
                  private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        val update = update()
        val signedTransaction: SignedTransaction = verifyAndSign(update)
        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")
        val session = initiateFlow(counterRef)
        session.send(clientInfo)
        val transactionSignedByAllParties = collectSignature(signedTransaction, listOf(session))
        return recordUpdate(transactionSignedByAllParties, listOf(session))
    }

    private fun update() : TransactionBuilder {
        val notary = inputStateRef().state.notary
        val updateCommand = Command(RegisterContract.Commands.Update(), outputState().participants.map { it.owningKey })
        val builder = TransactionBuilder(notary)
        builder.addInputState(inputStateRef())
        builder.addOutputState(outputState(), RegisterContract.REGISTER_CONTRACT_ID)
        builder.addCommand(updateCommand)
        return builder
    }

    private fun outputState(): RegisterState {
        val input = inputStateRef().state.data
        if (clientInfo.firstName == "")
            clientInfo.firstName = input.clientInfo.firstName

        if (clientInfo.lastName == "")
            clientInfo.lastName = input.clientInfo.lastName

        if (clientInfo.age == "")
            clientInfo.age = input.clientInfo.age

        if (clientInfo.gender == "")
            clientInfo.gender = input.clientInfo.gender

        if (clientInfo.address == "")
            clientInfo.address = input.clientInfo.address

        if (!input.verify)
            throw FlowException("The registrant must be approved before it can be update.")

        val counterRef = serviceHub.identityService.partiesFromName(counterParty, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for Owner $counterParty.")

        return RegisterState(clientInfo, ourIdentity, counterRef,true, linearId = linearId)
    }

    private fun inputStateRef(): StateAndRef<RegisterState> {
        val ref = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<RegisterState>(ref).states.single()
    }

    private fun verifyAndSign(transaction: TransactionBuilder) : SignedTransaction {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    @Suspendable
    private fun collectSignature (transaction: SignedTransaction, session: List<FlowSession>) :
            SignedTransaction = subFlow(CollectSignaturesFlow(transaction, session))

    @Suspendable
    private fun recordUpdate(transaction: SignedTransaction, session: List<FlowSession>) :
            SignedTransaction = subFlow(FinalityFlow(transaction, session))

}

@InitiatedBy(UpdateRegisterFlow::class)
class UpdateRegisterResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession)
        {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be UpdateRegister" using (output is RegisterState)
            }
        }

        val payload = flowSession.receive(ClientInfo::class.java).unwrap { it }
        val signedTransaction = subFlow(signedTransactionFlow)
        subFlow(RegisterFlow(payload))
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))

    }
}

