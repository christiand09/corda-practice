package com.template.states


import net.corda.core.contracts.Contract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party


abstract class ProjState : Contract {

    data class ProjState(
            val ownParty: Party,
            val name: String,
            val age: Int,
            val status: String,
            val isVerified: Boolean,
            override val participants: List<Party>,
            override  val linearId: UniqueIdentifier) : LinearState
}


