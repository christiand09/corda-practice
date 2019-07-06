package com.template.contracts

import com.template.states.KYCState
import com.template.states.PersonalState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction



class PersonalContract : Contract {

    companion object  {
        const val PERSONALID = "com.template.contracts.PersonalContract"
    }
    interface  Commands : CommandData {
        class Register : Commands
        class Verify : Commands
    }

     override fun verify(tx: LedgerTransaction){
        val command= tx.getCommand<CommandData>(0)
        requireThat {
            when(command.value){
                is Commands.Register -> {
                    "No inputs should be consumed when creating Personal." using (tx.inputs.isEmpty())
                    "Only one output state should be created" using (tx.outputs.size == 1)
                    "Output must be a PersonalState" using (tx.getOutput(0) is PersonalState)
                    val outputRegister = tx.outputsOfType<PersonalState>().single()
                    "Must be signed by the Registering node" using (command.signers.toSet() == outputRegister.participants.map { it.owningKey }.toSet())
                    "Name must not be empty" using (outputRegister.fullName.isNotEmpty())
                    "Age must not be empty" using (outputRegister.age >=1)
                    "Address must not be empty" using (outputRegister.address.isNotEmpty())
                    "Birthday must not be empty" using (outputRegister.birthDate.isNotEmpty())
                    "Status must not be empty" using (outputRegister.status.isNotEmpty())
                    "Validation must be defaulted into false" using (!outputRegister.verify)


                }
                is Commands.Verify -> {

                }
            }
        }

    }

}