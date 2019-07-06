package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.PersonalContract
import com.template.contracts.PersonalContract.Companion.PERSONALID
import com.template.states.PersonalState
import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

@InitiatingFlow
@StartableByRPC
class PersonalCreateFlow(private val fullName: String,
                         private val age : Int,
                         private val birthDate : String,
                         private val address : String,
                         private val contactNumber: Int,
                         private val status : String) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val transaction = transaction()
        val signedTransaction = verifyAndSign(transaction)
        return recordTransaction(signedTransaction)
    }
    //session if forward
    //val session = initiateFlow(state.lender)
    //Creating the Transaction
    private fun transaction(): TransactionBuilder {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val inputStatesRef = serviceHub.vaultService.queryBy<PersonalState>().states
        val searchName = inputStatesRef.find { stateAndRef -> stateAndRef.state.data.fullName == fullName }

        lateinit var outputState: ContractState

        if(searchName == null){
            outputState = PersonalState(
                    fullName,
                    age,
                    birthDate,
                    address,
                    contactNumber,
                    status,
                    ourIdentity,
                    ourIdentity,
                    false,
                    UniqueIdentifier()
            )
        }else{
            print("name found")

        }


        val issueCommand = Command(PersonalContract.Commands.Register(), ourIdentity.owningKey)
        val builder = TransactionBuilder(notary)
        builder.addOutputState(outputState, PERSONALID)
        builder.addCommand(issueCommand)
        return builder
    }

    //Verifying and sigining the Transaction
    private fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        System.out.println("collect Signed")
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    //Send the Transaction to the Notary
    //Record the Transaction to the initiator's vault
    //Broadcast to the participants of the transaction to record it to their vaults
    @Suspendable
    private fun recordTransaction(transaction: SignedTransaction): SignedTransaction =
            subFlow(FinalityFlow(transaction, emptyList()))

}