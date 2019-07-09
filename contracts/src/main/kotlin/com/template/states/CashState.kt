package com.template.states

import com.template.contracts.CashContract
import net.corda.core.contracts.*
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(CashContract::class)
data class CashState(val amount: Long,
                     val request: Boolean,
                     val status: String,
                     val borrower: Party,
                     val lender: Party,
                     val wallet: Long,
                     override val participants: List<Party> = listOf(borrower, lender),
                     override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState




