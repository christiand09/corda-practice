//package com.template.states
//
//import com.template.contracts.HouseContract
//import com.template.contracts.TemplateContract
//import jdk.nashorn.internal.parser.TokenType
//import net.corda.core.contracts.Amount
//import net.corda.core.contracts.BelongsToContract
//import net.corda.core.contracts.ContractState
//import net.corda.core.contracts.UniqueIdentifier
//import net.corda.core.identity.AbstractParty
//import net.corda.core.identity.Party
//import net.corda.core.serialization.CordaSerializable
//
//// *********
//// * State *
//// *********
//@BelongsToContract(HouseContract::class)
//
//@CordaSerializable
//
//data class HouseState(val owner: Party,
//                      val address: String,
//                      val valuation: Amount<TokenType>,
//                      override val maintainers: List<Party>,
//                      override val fractionDigits: Int = 0,
//                      override val linearId: UniqueIdentifier) : EvolvableTokenType()
