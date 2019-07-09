package com.template.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction

class CashContract : Contract
{
    companion object
    {
        @JvmStatic
        val CASH_ID = "com.template.contracts.CashContract"
    }

    override fun verify(tx: LedgerTransaction) {

    }

    interface Commands : CommandData
    {
        class Register: TypeOnlyCommandData(), Commands
        class Issue : TypeOnlyCommandData(), Commands
        class SelfIssue : TypeOnlyCommandData(), Commands
        class Transfer : TypeOnlyCommandData(), Commands
        class Approved : TypeOnlyCommandData(), Commands
    }
}