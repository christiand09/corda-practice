package com.template.webserver

import com.template.model.AttachmentModel
import com.template.states.AttachmentState
import com.template.webserver.utilities.FlowHandlerCompletion
import net.corda.core.crypto.SecureHash
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.AttachmentId
import net.corda.core.node.services.vault.AttachmentQueryCriteria
import net.corda.core.node.services.vault.Builder
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val CONTROLLER_NAME = "config.controller.name"
//@Value("\${$CONTROLLER_NAME}") private val controllerName: String
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class AttachmentController(
        private val rpc: NodeRPCConnection,
        private val flowHandlerCompletion : FlowHandlerCompletion
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy


    /**
     * Return all UserAccountState
     */

    @GetMapping(value = "/attachment/user", produces = ["application/json"])
    private fun getUserAccountStates(): ResponseEntity<Map<String, Any>> {
        val (status, result) = try {
            val requestStateRef = rpc.proxy.vaultQueryBy<AttachmentState>().states
            val requestStates = requestStateRef.map { it.state.data }
            val list = requestStates.map {
                AttachmentModel(
                        hash = it.hash,
                        linearId = it.linearId

                )
            }
            HttpStatus.CREATED to list
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful in getting ContractState of type UserAccountState"
        } else {
            "message" to "Failed to get ContractState of type UserAccountState"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }

    //Uploading Attachment


    @PostMapping(value = "attachment")
    private fun upload(@RequestParam file: MultipartFile, @RequestParam uploader: String): ResponseEntity<String> {
        val filename = file.originalFilename
        val hash: SecureHash =
                proxy.uploadAttachmentWithMetadata(
                        jar = file.inputStream,
                        uploader = uploader,
                        filename = filename!!
                )
        return ResponseEntity.created(URI.create("attachments/$hash")).body("Attachment uploaded with hash - $hash")
    }


    //Downloading by Hash/ AttachmentId



    @GetMapping("attachment/{hash}")
    fun downloadByHash(@PathVariable hash: String): ResponseEntity<Resource> {
        val inputStream = InputStreamResource(proxy.openAttachment(SecureHash.parse(hash)))
        return ResponseEntity.ok().header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"$hash.zip\""
        ).body(inputStream)
    }

    //downloading by Name



    @GetMapping("attachment/name")
    fun downloadByName(@RequestParam name: String): ResponseEntity<Resource> {
        val attachmentIds: List<AttachmentId> = proxy.queryAttachments(
                AttachmentQueryCriteria.AttachmentsQueryCriteria(filenameCondition = Builder.equal(name)),
                null
        )
        val inputStreams = attachmentIds.map { proxy.openAttachment(it) }
        val zipToReturn = if (inputStreams.size == 1) {
            inputStreams.single()
        } else {
            combineZips(inputStreams, name)
        }
        return ResponseEntity.ok().header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"$name.zip\""
        ).body(InputStreamResource(zipToReturn))
    }

    private fun combineZips(inputStreams: List<InputStream>, filename: String): InputStream {
        val zipName = "$filename-${UUID.randomUUID()}.zip"
        FileOutputStream(zipName).use { fileOutputStream ->
            ZipOutputStream(fileOutputStream).use { zipOutputStream ->
                inputStreams.forEachIndexed { index, inputStream ->
                    val zipEntry = ZipEntry("$filename-$index.zip")
                    zipOutputStream.putNextEntry(zipEntry)
                    inputStream.copyTo(zipOutputStream, 1024)
                }
            }
        }
        return try {
            FileInputStream(zipName)
        } finally {
            Files.deleteIfExists(Paths.get(zipName))
        }
    }



}