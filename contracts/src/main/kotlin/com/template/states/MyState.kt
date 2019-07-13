package com.template.states

import com.template.contracts.MyContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import com.template.states.AttachmentState

@BelongsToContract(MyContract::class)

data class MyState (var formSet : formSet,
                    var sender: Party,
                    val receiver: Party,
                    val spy: Party,
                    val wallet: Int,
                    val amountdebt: Int,
                    val amountpaid: Int,
                    val status: String = "Registered but not yet Approved",
                    val debtFree : Boolean = true,
                    val approvals: Boolean = false,
                    override val participants: List<Party> = listOf(sender, receiver),
                    override  val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

}


@CordaSerializable
data class formSet (
        var firstName: String,
        var lastName: String,
        var age: String,
        var gender: String,
        var address: String)

//@CordaSerializable
//data class Wallet (
//         var sender: Party,
//         val receiver: Party,
//         val cash: Int)
