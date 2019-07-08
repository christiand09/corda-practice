package com.template.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class MyContract : Contract {
    companion object {
        const val ID = "com.template.contracts.MyContract"
    }
    interface Commands : CommandData{
        class Register : Commands
        class Verify : Commands
        class Update : Commands
        class UpdateVerify: Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.getCommand<CommandData>(0)
        requireThat {
            when (command.value){
                is Commands.Register -> {

                }
                is Commands.Verify -> {

                }
                is Commands.Update -> {

                }
                is Commands.UpdateVerify -> {

                }
            }
        }
    }
}