package com.template.states

import com.template.contracts.TokenContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(TokenContract::class)
data class TokenState(val details:Details,
                      val requeststatus: Boolean,
                      val lender: Party,
                      val borrower:Party,
                      val walletbalance: Long,
                      override val participants: List<Party> = listOf(lender,borrower),
                      override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState
{
    fun total(totalamount: Long) = copy(walletbalance = walletbalance.plus(totalamount))
}

@CordaSerializable
data class Details(val name:String,
                   val address: String)






