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


                }
                is Commands.Verify -> {

                }
            }
        }

    }

}