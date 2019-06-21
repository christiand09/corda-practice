package com.template.contracts

import com.template.states.IOUState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class IOUContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        // const val ID = "com.template.contracts.TemplateContract"
        val IOU_CONTRACT_ID = IOUContract::class.qualifiedName!!
    }

    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
        class Transfer : TypeOnlyCommandData(), Commands
        class Settle : TypeOnlyCommandData(), Commands
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<IOUContract.Commands>()
        when (command.value) {
            is Commands.Issue -> requireThat {
                "No inputs should be consumed when issuing an IOU." using (tx.inputs.isEmpty())
                "Only one output state should be created when issuing an IOU." using (tx.outputs.size == 1)
                val iou = tx.outputStates.single() as IOUState
                "A newly issued IOU must have a positive amount." using (iou.amount.quantity > 0)
                "The lender and borrower cannot have the same identity." using (iou.borrower != iou.lender)
                "Both lender and borrower together only may sign IOU issue transaction." using
                        (command.signers.toSet() == iou.participants.map { it.owningKey }.toSet())
            }
            is Commands.Transfer -> requireThat {
                TODO()
            }
            is Commands.Settle -> {
                TODO()
            }
        }
    }
}