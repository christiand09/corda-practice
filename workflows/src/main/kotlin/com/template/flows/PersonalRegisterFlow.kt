package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.PersonalContract
import com.template.contracts.PersonalContract.Companion.PERSONALID
import com.template.states.PersonalState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC

class PersonalRegisterFlow(private val fullName: String,
                           private val age : Int,
                           private val birthDate : String,
                           private val address : String,
                           private val contactNumber: Int,
                           private val status : String ) : FlowLogic<SignedTransaction>(){

    @Suspendable
    override fun call(): SignedTransaction {
        val personalRegister = personalRegister (outputState())
        val signedTransaction = verifyAndSign (personalRegister)
        val session = emptyList<FlowSession>()
        val transactionSigned : SignedTransaction = collectSignature (signedTransaction, session)
        return recordTransaction (transactionSigned,session)
    }

private fun outputState () : PersonalState {
    return PersonalState(fullName,age,birthDate,address,contactNumber,status,ourIdentity,ourIdentity,false)
}
    private fun personalRegister(state:PersonalState):TransactionBuilder{
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val inputStatesRef = serviceHub.vaultService.queryBy<PersonalState>().states
        val searchName = inputStatesRef.find { stateAndRef -> stateAndRef.state.data.fullName != fullName }
                ?: throw IllegalArgumentException("error!")



        val cmd = Command (PersonalContract.Commands.Register(),ourIdentity.owningKey)
        return TransactionBuilder(notary)
                .addOutputState(state, PERSONALID)
                .addCommand(cmd)
    }

    private fun verifyAndSign(transactionBuilder: TransactionBuilder): SignedTransaction{
        transactionBuilder.verify(serviceHub)
        return serviceHub.signInitialTransaction(transactionBuilder)

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

