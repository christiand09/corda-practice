package com.template.models

import com.fasterxml.jackson.annotation.JsonCreator
import net.corda.core.contracts.UniqueIdentifier

data class WalletModel(
        val wallet: Long,
        val amountIssued: Long,
        val amountPaid: Long,
        val status: Boolean,
        val borrower: String,
        val lender: String,
        val admin: String,
        val linearId: UniqueIdentifier
)

data class WalletSelfIssueModel @JsonCreator constructor(
        val selfIssueAmount: Long,
        val linearId: UniqueIdentifier
)

data class WalletIssueModel @JsonCreator constructor(
        val amountIssue: Long,
        val counterParty: String,
        val linearId: UniqueIdentifier
)

data class WalletTransferModel @JsonCreator constructor(
        val amountTransfer: Long,
        val counterParty: String,
        val linearId: UniqueIdentifier
)

data class WalletSettleModel @JsonCreator constructor(
        val amountSettle: Long,
        val counterParty: String,
        val linearId: UniqueIdentifier
)