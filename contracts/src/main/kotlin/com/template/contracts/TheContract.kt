package com.template.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction


abstract class TheContract : Contract {

    companion object {
        const val The_Contract_ID ="com.template.contracts.UserContract"
    }
    interface Commands : CommandData {
        class Register : Commands
        class Update : Commands
        class Verify : Commands
        class Approved: Commands
        class Request : Commands
        class Remove : Commands
    }

    override fun verify(tx: LedgerTransaction){
        val command = tx.getCommand<CommandData>(0)
        requireThat {
            when(command.value){
                is Commands.Register -> {
//                    "No inputs should be consumed when issuing an IOU." using (tx.inputs.isEmpty())
//                    "Only one output state should be creating a record" using (tx.outputs.size == 1)
//                    "Output must be a TokenState" using (tx.getOutput(0) is TheContract.TheState)

                }
                is Commands.Update -> {
//

                }
                is Commands.Verify ->{

                }


                is Commands.Request ->{

                }

                is Commands.Approved ->{

                }

            }
        }
    }


}