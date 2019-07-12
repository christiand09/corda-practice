package com.template.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.template.states.Details
import net.corda.core.contracts.UniqueIdentifier

data class TokenModel(val details:Details,val lender:String,val borrower: String,
                      val requeststatus: String, val walletbalance: Long,
                      val amountborrowed: Long, val amountpaid: Long, val linearId: UniqueIdentifier)
data class addUser @JsonCreator constructor(
        val user: Details
)
data class tokenselfIssue @JsonCreator constructor(
        val amount: Long,
        val linearId: UniqueIdentifier
)
data class tokenissuetoanother @JsonCreator constructor(
        val amount: Long,
        val lender: String,
        val linearId: UniqueIdentifier
)
data class tokentransfer @JsonCreator constructor(
        val linearId: UniqueIdentifier,
        val borrower: String
)
data class tokensettlebalance @JsonCreator constructor(
        val linearId: UniqueIdentifier,
        val lender: String,
        val amount: Long
)