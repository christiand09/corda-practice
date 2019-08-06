package com.template.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction


class FunctionContract : Contract {

    companion object {
        const val ID = "com.template.contracts.FunctionContract"
    }

    interface Commands : CommandData{
        class All : Commands
    }

    override fun verify (tx: LedgerTransaction ) {
        val command = tx.getCommand<CommandData>(0)
        requireThat {
            when (command.value){
                is Commands.All -> {

                }
            }
        }
    }
}