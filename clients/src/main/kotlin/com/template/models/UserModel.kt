package com.template.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.template.states.Name
import net.corda.core.contracts.UniqueIdentifier

data class UserModel (
    val name: Name,
    val approved: Boolean,
    val linearId: UniqueIdentifier
)

data class UserRegisterModel @JsonCreator constructor (
    val name: Name
)

data class UserVerifyModel @JsonCreator constructor(
    val counterParty: String,
    val linearId: UniqueIdentifier
)

data class UserUpdateModel @JsonCreator constructor(
    val name: Name,
    val counterParty: String,
    val linearId: UniqueIdentifier
)

data class UserUpdateRegisterModel @JsonCreator constructor(
    val name: Name,
    val counterParty: String,
    val linearId: UniqueIdentifier
)