package com.template.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.template.states.Name
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import org.springframework.web.multipart.MultipartFile

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

data class AttachmentModel @JsonCreator constructor(
    val hash: SecureHash.SHA256,
    val linearId: UniqueIdentifier
)

data class AttachmentFlowModel @JsonCreator constructor(
    val counterParty: String,
    val hash: String
)

data class AttachmentUploadModel @JsonCreator constructor(
    val file: MultipartFile,
    val uploader: String
)
//data class AttachmentFlowModel @JsonCreator constructor(
//
//)