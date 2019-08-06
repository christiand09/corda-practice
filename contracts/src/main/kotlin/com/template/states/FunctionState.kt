package com.template.states

import com.template.contracts.FunctionContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(FunctionContract::class)
class FunctionState(
        val name: String,
        val status: Boolean,
        private val sender: Party,
        private val receiver: Party,
        override val linearId: UniqueIdentifier = UniqueIdentifier(),
        override val participants: List<Party> = listOf(sender,receiver)
): LinearState