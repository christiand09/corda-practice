package com.template.contracts

import com.template.states.UserState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction


class UserContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID_Contracts = "com.template.contracts.UserContract"
    }

    interface Commands : CommandData {
        class Register :TypeOnlyCommandData(), Commands
        class Verify:TypeOnlyCommandData(), Commands
        class Update:TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.getCommand<CommandData>(0)

        requireThat {

            when (command.value) {

                is Commands.Register -> {
                    "No inputs should be consumed when issuing an IOU" using (tx.inputs.isEmpty())
                    "Only one output state should be creating a record" using (tx.outputs.size == 1)
                    "Output must be a TokenState" using (tx.getOutput(0) is UserState)
                }
                is Commands.Verify -> {
                    val inputValidate = tx.inputsOfType<UserState>()
                    val outputValidate = tx.outputsOfType<UserState>()
                    val inputValidateState = inputValidate.single()
                    val outputValidateState = outputValidate.single()
                    "There can only be one output consumed when validating" using (outputValidate.size == 1)
                    "There can only be one input consumed when validating " using(inputValidate.size == 1)
                    "Input must be an UserState" using(tx.inputStates[0] is UserState)
                    "Output must be an UserState" using(tx.outputStates[0] is UserState)
                    "Input verified must be true" using(inputValidate.single().verify)
                    "Output verified must be true" using (outputValidate.single().verify)
                    "Firstname input and outputstate" using(inputValidateState.fullname == outputValidateState.fullname)
                }
                is Commands.Update ->
                {

                }
            }
        }
    }
    // Verification logic goes here.


    // Used to indicate the transaction's intent.

}