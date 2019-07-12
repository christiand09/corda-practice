package com.template.models

import net.corda.core.contracts.UniqueIdentifier

data class CashModel(
        val wallet: Long,
        val amountIssued: Long,
        val amountPaid: Long,
        val status: Boolean,
        val borrower: String,
        val lender: String,
        val admin: String,
        val linearId: UniqueIdentifier
)

data class CashSelfIssueModel(
        val selfIssueAmount: Long,
        val linearId: UniqueIdentifier
)