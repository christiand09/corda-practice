package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.PersonalContract
import com.template.contracts.PersonalContract.Companion.PERSONALID
import com.template.states.PersonalState
import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
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
        val personalRegister = personalRegister ()
        val signedTransaction = verifyAndSign (personalRegister)
        val session = emptyList<FlowSession>()
        val transactionSigned : SignedTransaction = collectSignature (signedTransaction, session)
        return recordTransaction (transactionSigned,session)
    }

    private fun personalRegister():TransactionBuilder{
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val inputStatesRef = serviceHub.vaultService.queryBy<PersonalState>().states
        val searchName = inputStatesRef.find { stateAndRef -> stateAndRef.state.data.fullName != fullName }

        lateinit var outputState: ContractState

        if(searchName == null){
            print("no name found")
        }else{
            outputState = PersonalState(fullName,age,birthDate,address,contactNumber,status,ourIdentity,ourIdentity,false)
        }


        val cmd = Command (PersonalContract.Commands.Register(),ourIdentity.owningKey)
        return TransactionBuilder(notary)
                .addOutputState(outputState, PERSONALID)
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

@InitiatedBy(PersonalRegisterFlow::class)
class PersonalREgisterFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction" using (output is PersonalState)
            }
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}