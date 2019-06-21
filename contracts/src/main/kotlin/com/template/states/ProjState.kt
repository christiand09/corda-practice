package com.template.states

import net.corda.core.contracts.Amount
import net.corda.core.contracts.Contract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import java.util.*

abstract class ProjState : Contract {

    data class ProjState(
            val amount: Amount<Currency>,
            val lender: Party,
            val borrower: Party,
            val paid: Amount<Currency> = Amount(0, amount.token),
            override val linearId: UniqueIdentifier = UniqueIdentifier(),
            override val participants: List<Party> = listOf(lender, borrower)
    ) : LinearState
}