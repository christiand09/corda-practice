package com.template.contracts


import com.template.states.RegisterState
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.*

class RegisterContract : Contract
{
    companion object
    {
        @JvmStatic
        val REGISTER_ID = "com.template.contracts.RegisterContract"
    }

    interface Commands : CommandData
    {
        class Register : TypeOnlyCommandData(), Commands
        class Verify: TypeOnlyCommandData(), Commands
        class Update: TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value)
        {
            is Commands.Register -> requireThat {
                "There can be no inputs when sending info to other parties" using (tx.inputs.isEmpty())
                "There must be one output when the info is send." using (tx.outputs.size == 1)
                val info = tx.outputsOfType<RegisterState>().single()
                "Info must be signed by the sender" using (info.sender.owningKey == command.signers.single())
            }
            is Commands.Verify -> requireThat {

            }
            is Commands.Update -> requireThat {

            }
        }
    }
}