package com.template.states

import com.template.contracts.TestContract
import com.template.contracts.WalletContract
import net.corda.core.contracts.*
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import java.sql.Time

@BelongsToContract(TestContract::class)
data class TestState(val status: Boolean,
                     val party: Party,
                     val counter: Party,
                     override val participants: List<Party> = listOf(party, counter),
                     override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState