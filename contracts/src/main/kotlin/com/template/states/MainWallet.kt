//package com.template.states
//
//import com.template.contracts.MyContract
//import net.corda.core.contracts.BelongsToContract
//import net.corda.core.contracts.ContractState
//import net.corda.core.contracts.LinearState
//import net.corda.core.contracts.UniqueIdentifier
//import net.corda.core.identity.AbstractParty
//import net.corda.core.identity.Party
//import net.corda.core.serialization.CordaSerializable
//import com.template.states.AttachmentState
//
//@BelongsToContract(MyContract::class)
//
//data class MainWallet ( var borrower: Party,
//                        val lender: Party,
//                        val cash: Int,
//                        val status: String = "Registered but not yet Approved",
//                        val approvals: Boolean =false,
//                        override val participants: List<Party> = listOf(borrower, lender),
//                        override  val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState
//
//
//
