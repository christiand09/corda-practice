package com.template.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction

class TestContract : Contract
{
    companion object
    {
        @JvmStatic
        val TEST_ID = "com.template.contracts.TestContract"
    }

    override fun verify(tx: LedgerTransaction) {

    }

    interface Commands : CommandData
    {
        class Register : TypeOnlyCommandData(), Commands
        class Issue : TypeOnlyCommandData(), Commands
        class Verify : TypeOnlyCommandData(), Commands
//        class SelfIssue : TypeOnlyCommandData(), Commands
//        class Transfer : TypeOnlyCommandData(), Commands
//        class Settle : TypeOnlyCommandData(), Commands
    }
}