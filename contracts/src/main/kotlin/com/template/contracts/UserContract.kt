package com.template.contracts

import com.template.states.UserState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey


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
        val setofsigners = command.signers.toSet()

            when (command.value) {
                is Commands.Register -> verifyRegister(tx,setofsigners)
                is Commands.Verify -> verifyVerification(tx,setofsigners)
                is Commands.Update -> verifyUpdate(tx,setofsigners)
            }
    }

   private fun keyformParticipants(userstate:UserState):Set<PublicKey>
   {
        return userstate.participants.map { it.owningKey }.toSet()
   }
    private fun verifyRegister(tx: LedgerTransaction,signers:Set<PublicKey>) = requireThat{
        "No inputs should be consumed when issuing an IOU" using (tx.inputs.isEmpty())
        "Only one output state should be creating a record" using (tx.outputs.size == 1)
        "Output must be a TokenState" using (tx.getOutput(0) is UserState)
    }
    private fun verifyVerification(tx: LedgerTransaction,signers:Set<PublicKey>) = requireThat {

        val inputValidate = tx.inputsOfType<UserState>()
        val outputValidate = tx.outputsOfType<UserState>()
        "There can only be one output consumed when validating" using (outputValidate.size == 1)
        "There can only be one input consumed when validating " using(inputValidate.size == 1)
        "Input must be an UserState" using(tx.inputStates[0] is UserState)
        "Output must be an UserState" using(tx.outputStates[0] is UserState)
        "Output verified must be true" using (outputValidate.single().verify)
    }
    private fun verifyUpdate(tx: LedgerTransaction,signers:Set<PublicKey>) = requireThat {

        val inputValidate = tx.inputsOfType<UserState>()
        val input = inputValidate.single()
        "Must be validated first before you can update" using(input.verify == true)
        "Cannot be updated by the counterparty" using(input.receiver != input.sender)
    }

}