package com.template.contracts

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class UserContract : Contract {

    companion object {
        @JvmStatic
        val USER_CONTRACT_ID = "com.template.contracts.UserContract"
    }

    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands
        class Verify : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Create -> requireThat {
//                "No inputs should be consumed when issuing an IOU." using (tx.inputs.isEmpty())
//                "Only one output state should be created when issuing an IOU." using (tx.outputs.size == 1)
//                val iou = tx.outputStates.single() as IOUState
//                "A newly issued IOU must have a positive amount." using (iou.amount.quantity > 0)
//                "The lender and borrower cannot have the same identity." using (iou.borrower != iou.lender)
//                "Both lender and borrower together only may sign IOU issue transaction." using
//                        (command.signers.toSet() == iou.participants.map { it.owningKey }.toSet())
            }
            is Commands.Verify-> requireThat {

            }

        }
    }
}