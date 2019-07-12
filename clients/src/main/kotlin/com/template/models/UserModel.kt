package com.template.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.template.states.UserState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import org.springframework.web.multipart.MultipartFile


data class UserModel(
        val name: UserState.Name,
        val verify: Boolean,
        val linearId: UniqueIdentifier

        )

data class createUserModel @JsonCreator constructor(
       var name: UserState.Name

)
data class verifyUserModel @JsonCreator constructor(
        val receiver: String,
        val linearId: UniqueIdentifier
)
data class updateUserModel @JsonCreator constructor(
        val name: UserState.Name,
        val receiver: String,
        val linearId: UniqueIdentifier
)
data class updateRegisterUserModel @JsonCreator constructor(
        val name: UserState.Name,
        val receiver: String,
        val linearId: UniqueIdentifier
)
data class uploadAttachmentModel @JsonCreator constructor(
        val receiver: String,
        val hash: String
        )
