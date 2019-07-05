package com.template.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.template.states.formSet
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party


data class userAccountModel(
        var formSet : formSet,
        val approvals: Boolean,
        val linearId: UniqueIdentifier
        )

data class createUserModel @JsonCreator constructor(
        var formSet : formSet
)

data class verifyUserModel @JsonCreator constructor(
        val receiver: String,
        val linearId: UniqueIdentifier = UniqueIdentifier()
)

data class updateUserModel @JsonCreator constructor(
        val formSet: formSet,
        val receiver: String,
        val linearId: UniqueIdentifier = UniqueIdentifier()
)
data class updateRegisterUserModel @JsonCreator constructor(
        val formSet: formSet,
        val receiver: String,
        val linearId: UniqueIdentifier = UniqueIdentifier()
)