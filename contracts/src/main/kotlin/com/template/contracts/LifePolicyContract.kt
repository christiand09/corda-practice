package com.template.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction

class LifePolicyContract : Contract
{
    companion object
    {
        @JvmStatic
        val LIFE_ID = "com.template.contracts.LifePolicyContract"
    }

    override fun verify(tx: LedgerTransaction) {

    }

    interface Commands : CommandData
    {
        class Register: TypeOnlyCommandData(), Commands
        class ApplyLoan: TypeOnlyCommandData(), Commands
    }
}