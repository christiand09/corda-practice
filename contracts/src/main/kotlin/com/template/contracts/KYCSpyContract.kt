package com.template.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class KYCSpyContract : Contract {

    companion object {
        const val ID = "com.template.contracts.KYCSpyContract"
    }
    interface Commands: CommandData {
        class Register : Commands
        class SelfIssue : Commands
        class Issue : Commands
        class Transfer : Commands
        class Settle : Commands
    }


    override fun verify(tx: LedgerTransaction) {
        val command = tx.getCommand<CommandData>(0)
        requireThat {
            when (command.value){
                is Commands.Register -> {

                }
                is Commands.SelfIssue -> {

                }
                is Commands.Issue -> {

                }
                is Commands.Transfer -> {

                }
                is Commands.Settle -> {

                }
            }
        }
    }

}