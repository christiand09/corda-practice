package com.template.states

import com.template.contracts.WalletContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(WalletContract::class)
data class WalletState (val name: String,
                        var walletBalance: Long,
                        var amountBorrowed: Long,
                        var issueStatus: String,
                        var amountPaid: Long,
                        var lender: Party,
                        var borrower: Party,
                        val admin: Party,
                        override val participants: List<Party> = listOf(lender,borrower),
                        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {
//    fun pay (amountToPay: Long) = copy(paid = paid.plus(amountToPay))
//    fun withNewLender (newLender: Party) = copy(lender = newLender)
}

