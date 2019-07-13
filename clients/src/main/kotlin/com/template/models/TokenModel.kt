package com.template.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.template.states.UserState
import net.corda.core.contracts.UniqueIdentifier


data class TokenModel(
        val amountIssued: Long,
        val amountPaid: Long,
        val borrower: String,
        val lender: String,
        val iss: String,
        val walletBalance: Long,
        val settled: Boolean,
        val linearId: UniqueIdentifier

)

data class TokenIssueModel @JsonCreator constructor(
        val amountIssued: Long,
        val counterParty: String,
        val linearId: UniqueIdentifier
)

data class TokenTransferModel @JsonCreator constructor(
        val amountToLend: Long,
        val counterParty: String,
        val linearId: UniqueIdentifier
)

data class TokenSettleModel @JsonCreator constructor(
        val amountToPay: Long,
        val counterParty: String,
        val linearId: UniqueIdentifier
)

data class TokenSelfIssueModel @JsonCreator constructor(
        val walletBalance: Long,
        val linearId: UniqueIdentifier
)

