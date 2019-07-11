package com.template.states

import com.template.contracts.WalletContract
import net.corda.core.contracts.*
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(WalletContract::class)
data class WalletState(val wallet: Long,
                       val amountIssued: Long,
                       val amountPaid: Long,
                       val status: Boolean,
                       val borrower: Party,
                       val lender: Party,
                       val admin: Party,
                       override val participants: List<Party> = listOf(borrower, lender),
                       override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState




