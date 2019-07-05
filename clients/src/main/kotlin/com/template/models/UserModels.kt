package com.template.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.template.states.UserDetails
import net.corda.core.contracts.UniqueIdentifier

data class UserModel(
        val name: UserDetails,
        val verify: Boolean,
        val linearId: UniqueIdentifier
)

data class addUsers @JsonCreator constructor(
        val name: UserDetails
)
data class verifyUser @JsonCreator constructor(
        val linearId: UniqueIdentifier,
        val counterparty: String
)
data class updateUser @JsonCreator constructor(
        val name: UserDetails,
        val counterparty: String,
        val linearId: UniqueIdentifier
)
data class updateandregister @JsonCreator constructor(
        val name: UserDetails,
        val counterparty: String,
        val linearId: UniqueIdentifier
)
