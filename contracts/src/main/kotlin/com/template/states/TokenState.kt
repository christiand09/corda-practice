package com.template.states


import com.template.contracts.TokenContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(TokenContract::class)
data class TokenState(
//        val amountToBorrow: Long,
        val amountIssued: Long,
        val amountPaid: Long,
        val borrower: Party,
        val lender: Party,
        val iss: Party,
        val walletBalance: Long,
//        val approve: Boolean = false,
        val settled: Boolean = false,
        override val linearId: UniqueIdentifier = UniqueIdentifier(),
        override val participants: List<Party> = listOf(borrower,lender)): LinearState








