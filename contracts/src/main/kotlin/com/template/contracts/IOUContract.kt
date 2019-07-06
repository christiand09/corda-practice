package com.template.contracts

import com.template.states.IOUState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class IOUContract : Contract {

    companion object {
        @JvmStatic
        val IOU_CONTRACT_ID = "com.template.contracts.IOUContract"
    }

    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
        class Transfer : TypeOnlyCommandData(), Commands
        class Settle : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<IOUContract.Commands>()
        when (command.value) {
            is Commands.Issue -> requireThat {
                "No inputs should be consumed when issuing an iou." using (tx.inputs.isEmpty())
                "Only one output state should be created when issuing an iou." using (tx.outputs.size == 1)
                val iou = tx.outputStates.single() as IOUState
                "A newly issued iou must have a positive amount." using (iou.amount.quantity > 0)
                "The lender and borrower cannot have the same identity." using (iou.borrower != iou.lender)
                "Both lender and borrower together only may sign iou issue transaction." using
                        (command.signers.toSet() == iou.participants.map { it.owningKey }.toSet())
            }
            is Commands.Transfer -> requireThat {
                // more conditions
            }
            is Commands.Settle -> {
                // more conditions
            }
        }
    }
}