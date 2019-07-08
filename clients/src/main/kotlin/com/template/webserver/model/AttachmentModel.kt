package com.template.webserver.model

import org.springframework.web.multipart.MultipartFile

data class AttachmentModel (
        val attachment: MultipartFile,
        val uploader: String
)
