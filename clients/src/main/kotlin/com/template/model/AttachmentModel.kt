package com.template.model

import com.fasterxml.jackson.annotation.JsonCreator
import net.corda.core.contracts.UniqueIdentifier

data class AttachmentModel @JsonCreator constructor(
        val attachId: String,
        val party: String,
        val counterParty: String,
        val linearId: UniqueIdentifier

)

data class AttachmentRegisterModel @JsonCreator constructor(
        val attachId: String,
        val counterParty: String
)

