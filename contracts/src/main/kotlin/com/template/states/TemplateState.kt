package com.template.states

import com.template.contracts.TemplateContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.util.*

// *********
// * State *
// *********
@BelongsToContract(TemplateContract::class)
data class TemplateState(val amount: Amount<Currency>,
                         val lender: Party,
                         val borrower: Party,
                         val paid: Amount<Currency> = Amount(0, amount.token),
                         override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    //To change initializer of created properties use File | Settings | File Templates.
    override val participants: List<Party> get() = TODO("To be implemented")
}

