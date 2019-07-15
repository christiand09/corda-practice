package com.template.models

import com.fasterxml.jackson.annotation.JsonCreator
import net.corda.core.contracts.UniqueIdentifier

data class CashModel (
        val name: String,
        val walletBalance: Long,
        val amountBorrowed: Long,
        val issueStatus: String,
        val amountPaid: Long,
        val lender: String,
        val borrower: String,
        val admin: String,
        val linearId: UniqueIdentifier)

data class WalletRegisterAccount @JsonCreator constructor(
        val name: String
)

data class WalletSelfIssueAccount @JsonCreator constructor(
        val amount: Long,
        val linearId: UniqueIdentifier
)

data class WalletIssueAccount @JsonCreator constructor(
        val amount: Long,
        val counterParty: String,
        val linearId: UniqueIdentifier
)

data class WalletTransferAccount @JsonCreator constructor(
        val amount: Long,
        val counterParty: String,
        val linearId: UniqueIdentifier
)

data class WalletSettleAccount @JsonCreator constructor(
        val amount: Long,
        val counterParty: String,
        val linearId: UniqueIdentifier
)