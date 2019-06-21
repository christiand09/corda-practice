package com.template.contracts

import com.template.states.ProjState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class ProjContract : Contract {

    companion object {
        @JvmStatic
        val Proj_CONTRACT_ID = "net.corda.training.contract.IOUContract"
    }

    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
        class Transfer : TypeOnlyCommandData(), Commands
        class Settle : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<ProjContract.Commands>()
        when (command.value) {
            is Commands.Issue -> requireThat {
                "No inputs should be consumed when issuing an IOU." using (tx.inputs.isEmpty())
                "Only one output state should be created when issuing an IOU." using (tx.outputs.size == 1)
                val iou = tx.outputStates.single() as ProjState.ProjState
                "A newly issued IOU must have a positive amount." using (iou.amount.quantity > 0)
                "The lender and borrower cannot have the same identity." using (iou.borrower != iou.lender)
                "Both lender and borrower together only may sign IOU issue transaction." using
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