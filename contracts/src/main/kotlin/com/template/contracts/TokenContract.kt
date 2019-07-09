package com.template.contracts

import com.template.states.TokenState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction

class TokenContract : Contract
{
    companion object
    {
        @JvmStatic
        val TOKEN_ID = "com.template.contracts.TokenContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val state = tx.outputsOfType<TokenState>().single()
    }
    interface Commands : CommandData {
        class Token :TypeOnlyCommandData(), Commands
    }
}