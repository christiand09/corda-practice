package com.template.model

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash

data class AttachmentModel(
        val hash : SecureHash.SHA256,
        val linearId : UniqueIdentifier
)

