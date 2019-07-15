package com.template.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.template.states.ClientInfo
import com.template.states.RegisterState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party

data class UserModel (
        val clientInfo: ClientInfo,
        val verify : Boolean,
        val linearId : UniqueIdentifier

)

data class UserAttachmentModel (
        val attachId: SecureHash.SHA256,
        val party1: String,
        val party2: String,
        val linearId: UniqueIdentifier
)

data class regUserAccount @JsonCreator constructor (
        val clientInfo: ClientInfo
)

data class verUserAccount @JsonCreator constructor(
        val counterParty: String,
        val linearId: UniqueIdentifier
)

data class upUserAccount @JsonCreator constructor(
        val clientInfo: ClientInfo,
        val counterParty: String,
        val linearId: UniqueIdentifier
)

data class upRegUserAccount @JsonCreator constructor(
        val clientInfo: ClientInfo,
        val counterParty: String,
        val linearId: UniqueIdentifier
)

data class attachmentUserAccount @JsonCreator constructor(
        val counterParty: String,
        val attachId: String
)