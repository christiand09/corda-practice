package com.template.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.template.states.Calls
import net.corda.core.contracts.UniqueIdentifier


data class ClientUserModel(
        val calls: Calls,
        val verify: Boolean,
        val linearId: UniqueIdentifier

)

data class CreateUserAccount @JsonCreator constructor(
        val calls: Calls
)

data class VerifyUserAccount @JsonCreator constructor(
        val counterparty: String,
        val linearId: UniqueIdentifier
)

data class UpdateUserAccount @JsonCreator constructor(
        val calls: Calls,
        val  counterparty: String,
        val linearId: UniqueIdentifier
)

data class UpdateRegisterAccount @JsonCreator constructor(
        val calls: Calls,
        val counterparty: String,
        val linearId: UniqueIdentifier
)

