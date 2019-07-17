package com.template.states

import com.template.contracts.KYCContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(KYCContract::class)
data class KYCState(
        val moneyLend : Long,
        val moneyBalance: Long,
        val requestedAmount: Long,
        val lender: Party,
        val borrower: Party,
        val status: String,
        override val linearId: UniqueIdentifier = UniqueIdentifier(),
        override val participants: List<Party> = listOf(lender, borrower)): LinearState

