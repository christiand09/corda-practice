package com.template.contracts

import com.template.states.RegisterState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class RegisterContract : Contract {

    companion object {
        @JvmStatic
        // Used to identify our contract when building a transaction.
        val REGISTER_CONTRACT_ID = RegisterContract::class.qualifiedName!!
    }


    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Register -> requireThat {
                "Only one output state should be created when registering" using (tx.outputs.size == 1)
                "The output must be RegisterState" using (tx.getOutput(0) is RegisterState)
                "No information should be send to other parties" using (tx.inputs.isEmpty())
                val info = tx.outputsOfType<RegisterState>().single()
                "The sender must be signed the info" using (info.sender.owningKey == command.signers.single())
            }
            is Commands.Verify -> requireThat {
                "Only one output state should be created when verifying" using (tx.outputs.size == 1)
                "Only one input state should be created when verifying" using (tx.inputs.size == 1)
                "The output must be RegisterState" using (tx.getOutput(0) is RegisterState)
                "The input must be RegisterState" using (tx.getOutput(0) is RegisterState)
            }
            is Commands.Update -> requireThat {

            }
            is Commands.UpdateRegister -> requireThat {

            }
        }
    }
    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Register : TypeOnlyCommandData(), Commands
        class Verify : TypeOnlyCommandData(), Commands
        class Update : TypeOnlyCommandData(), Commands
        class UpdateRegister : TypeOnlyCommandData(), Commands
    }
}