package com.template.states

import net.corda.core.contracts.Contract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party


abstract class TheContract : Contract {

    data class TheState(
            val name: String,
            val age : Int,
            val sender: Party,
            val receiver: Party,
            override val linearId: UniqueIdentifier = UniqueIdentifier(),
            override val participants: List<Party> = listOf(sender, receiver)
    ) : LinearState
}