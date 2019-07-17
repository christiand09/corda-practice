package com.template.states

import com.template.contracts.KYCSpyContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(KYCSpyContract::class)
data class KYCSpyState(
        val moneyLend : Long,
        val moneyBalance: Long,
        val requestedAmount: Long,
        val lender: Party,
        val borrower: Party,
        val spy: Party,
        val status: String,
        override val linearId: UniqueIdentifier = UniqueIdentifier(),
        override val participants: List<Party> = listOf(lender, borrower)): LinearState

