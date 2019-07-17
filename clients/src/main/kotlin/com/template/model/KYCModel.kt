package com.template.model

import com.fasterxml.jackson.annotation.JsonCreator
import net.corda.core.contracts.UniqueIdentifier

data class KYCUserModel @JsonCreator constructor(

        val moneyLend : Long,
        val moneyBalance: Long,
        val requestedAmount: Long,
        val lender: String,
        val borrower: String,
        val spy: String,
        val status: String,
        val linearId: UniqueIdentifier

)

data class KYCIssueModel @JsonCreator constructor (
        val amount: Long,
        val linearId: UniqueIdentifier
)

data class KYCRequestModel @JsonCreator constructor (
        val amount: Long,
        val lender: String,
        val linearId: UniqueIdentifier
)
data class KYCTransferModel @JsonCreator constructor (
        val borrower: String,
        val linearId: UniqueIdentifier
)
data class KYCSettleModel @JsonCreator constructor (
        val amountPay : Long,
        val lender: String,
        val linearId: UniqueIdentifier
)


