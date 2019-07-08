package com.template.webserver

import com.template.webserver.model.AttachmentModel
import com.template.webserver.model.ResponseModel
import com.template.webserver.utilities.FlowHandlerCompletion
import net.corda.core.crypto.SecureHash
import net.corda.core.node.services.AttachmentId
import org.slf4j.LoggerFactory
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

/**
 * Define your API endpoints here.
 */
@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection,
                 flowHandlerCompletion: FlowHandlerCompletion) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    private val myIdentity = rpc.proxy.nodeInfo().legalIdentities.first().name

    @GetMapping(value = ["/me"], produces = ["application/json"])
    private fun myName() = mapOf("me" to myIdentity.toString())


    @GetMapping(value = ["/templateendpoint"], produces = ["text/plain"])
    private fun templateendpoint(): String {
        return "Define an endpoint here."
    }


    @PostMapping(value = ["/upload"])
    private fun upload(@RequestBody file: MultipartFile, @RequestParam uploader: String): ResponseEntity<ResponseModel> {

       try {
            val filename = file.originalFilename
            val hash: SecureHash =
                    proxy.uploadAttachmentWithMetadata(
                            jar = file.inputStream,
                            uploader = uploader,
                            filename = filename!!
                    )
           val res = ResponseModel(status = "success", message = "successful in uploading attachment",
                   result = "Attachment uploaded with hash - $hash")
           return ResponseEntity.created(URI.create("attachments/$hash")).body(res)
        }catch (e: Exception){
            HttpStatus.CREATED to "failed"
           val res = ResponseModel(status = "failed", message = "unsuccessful in uploading attachment",
                   result = e.message
           )
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res)
        }


    }


}