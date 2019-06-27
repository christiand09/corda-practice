package com.template.contracts


import com.template.states.UserState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class UserContract : Contract {

    companion object {
        @JvmStatic
        val REGISTER_ID = "com.template.contracts.UserContract"
    }

    interface Commands : CommandData {
        class Register : Commands
        class Verify : Commands
        class Update : Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<UserContract.Commands>()
        when (command.value) {
            is Commands.Register -> requireThat {

            }
            is Commands.Verify -> requireThat {

            }
            is Commands.Update -> requireThat {

            }
            else -> requireThat {
                // more conditions
            }

        }
    }
}