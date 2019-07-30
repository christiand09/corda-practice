package com.template.flows.schedulable

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

open class schedulableContract : Contract {
    companion object {
        const val contractID = "com.template.flows.schedulable.schedulableContract"
    }
    override fun verify(tx: LedgerTransaction) {
        // Omitted for the purpose of this sample.
    }
    interface Commands : CommandData {
        class first : Commands
        class second : Commands
    }
}