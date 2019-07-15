package com.template.webserver

import com.template.flows.clientFlows.AttachmentFlow
import com.template.models.*
import com.template.states.AttachmentState
import com.template.states.RegisterState
import net.corda.core.crypto.SecureHash
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.net.URI

/**
 * Define your API endpoints here.
 */

private const val CONTROLLER_NAME = "config.controller.name"
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(private val rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = "/states/user", produces = arrayOf("application/json"))
    private fun getClientStates(): ResponseEntity<Map<String,Any>> {
        val (status, result) = try {
            val registerStateRef = rpc.proxy.vaultQueryBy<RegisterState>().states
            val requestStates = registerStateRef.map { it.state.data }
            val list = requestStates.map {
                UserModel (
                        clientInfo = it.clientInfo,
                        verify = it.verify,
                        linearId = it.linearId
                )
            }
            HttpStatus.CREATED to list
        } catch ( e: Exception ) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful!"
        } else {
            "message" to "Failed!"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }


    /**
    *   REGISTER - Register Flow
    */
    @PostMapping(value = "/states/user/register", produces = arrayOf("application/json"))
    private fun regUserAccount(@RequestBody regUserAccount: regUserAccount) : ResponseEntity<Map<String,Any>> {

        val (status, result) = try {
            val user = regUserAccount(clientInfo = regUserAccount.clientInfo)

            proxy.startFlowDynamic(RegisterFlow::class.java,
                    user.clientInfo)


            HttpStatus.CREATED to regUserAccount
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful!"
        } else {
            "message" to "Failed!"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }


    /**
     *   VERIFY - Verify Flow
     */
    @PostMapping(value = "states/user/verify", produces = arrayOf("application/json"))
    private fun verUserAccount (@RequestBody verUserAccount: verUserAccount) : ResponseEntity<Map<String,Any>> {

        val (status, result) = try {
            val verify = verUserAccount(counterParty = verUserAccount.counterParty,
                    linearId = verUserAccount.linearId)

            proxy.startFlowDynamic(VerifyFlow::class.java,
                    verify.counterParty,
                    verify.linearId)

            HttpStatus.CREATED to verUserAccount
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful!"
        } else {
            "message" to "Failed!"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }


    /**
     *   UPDATE - Update Flow
     */
    @PostMapping(value = "states/user/update", produces = arrayOf("application/json"))
    private fun upUserAccount (@RequestBody upUserAccount: upUserAccount) : ResponseEntity<Map<String, Any>> {

        val (status, result) = try {
            val update = upUserAccount(clientInfo = upUserAccount.clientInfo,
                    counterParty = upUserAccount.counterParty,
                    linearId = upUserAccount.linearId)

            proxy.startFlowDynamic(UpdateFlow::class.java,
                    update.clientInfo,
                    update.counterParty,
                    update.linearId)
            HttpStatus.CREATED to upUserAccount
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful!"
        } else {
            "message" to "Failed!"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }


    /**
     *   UPDATEREGISTER - UpdateRegister Flow
     */
    @PostMapping(value = "states/user/updateregister", produces = arrayOf("application/json"))
    private fun upRegUserAccount (@RequestBody upRegUserAccount: upRegUserAccount) : ResponseEntity<Map<String, Any>> {

        val (status, result) = try {
            val updateReg = upRegUserAccount(clientInfo = upRegUserAccount.clientInfo,
                    counterParty = upRegUserAccount.counterParty,
                    linearId = upRegUserAccount.linearId)

            proxy.startFlowDynamic(UpdateRegisterFlow::class.java,
                    updateReg.clientInfo,
                    updateReg.counterParty,
                    updateReg.linearId)
            HttpStatus.CREATED to upRegUserAccount
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful!"
        } else {
            "message" to "Failed!"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }


    /**
     *   ATTACHMENT - Attachment Flow
     */
    @GetMapping(value = "/states/userAttachment", produces = arrayOf("application/json"))
    private fun getClientAttachmentStates(): ResponseEntity<Map<String,Any>> {
        val (status, result) = try {
            val attachmentStateRef = rpc.proxy.vaultQueryBy<AttachmentState>().states
            val requestStates = attachmentStateRef.map { it.state.data }
            val list = requestStates.map {
                UserAttachmentModel (
                        attachId = it.attachId,
                        party1 = it.party1.toString(),
                        party2 = it.party2.toString(),
                        linearId = it.linearId
                )
            }
            HttpStatus.CREATED to list
        } catch ( e: Exception ) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful!"
        } else {
            "message" to "Failed!"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }

    @PostMapping(value = "states/attachment", produces = arrayOf("application/json"))
    private fun attachmentUserAccount (@RequestBody attachmentUserAccount: attachmentUserAccount) : ResponseEntity<Map<String, Any>> {

        val (status, result) = try {
            val attachment = attachmentUserAccount(counterParty = attachmentUserAccount.counterParty,
                    attachId = attachmentUserAccount.attachId)

            proxy.startFlowDynamic(AttachmentFlow::class.java,
                    attachment.counterParty, attachment.attachId)
            HttpStatus.CREATED to attachmentUserAccount
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful!"
        } else {
            "message" to "Failed!"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }



    /**
     *   UPLOAD ATTACHMENTS
     */
    @PostMapping (value = "/uploadAttachments")
    fun upload(@RequestParam file: MultipartFile,
               @RequestParam uploader: String): ResponseEntity<String> {

        val filename = file.originalFilename
        val hash: SecureHash = proxy.uploadAttachmentWithMetadata(jar = file.inputStream,
                uploader = uploader,
                filename = filename!!)
        return ResponseEntity.created(URI.create("attachments/$hash")).body("Attachment uploaded with hash - $hash")
    }


    /**
     *   DOWNLOAD ATTACHMENTS - by HASH
     */
    @GetMapping("/downloadAttachments/{hash}")
    fun downloadByHash(@PathVariable hash: String): ResponseEntity<InputStreamResource> {
        val inputStream = InputStreamResource(proxy.openAttachment(SecureHash.parse(hash)))
        return ResponseEntity.ok().header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"$hash.zip\""
        ).body(inputStream)
    }


    /**
     *   DOWNLOAD ATTACHMENTS - by NAME
     */
//    @GetMapping("downloadAttachments/")
//    fun downloadByName(@RequestParam name: String): ResponseEntity<InputStreamResource> {
//        val attachId: List<AttachmentId> = proxy.queryAttachments(AttachmentQueryCriteria.AttachmentsQueryCriteria(Builder.equal(name)),
//                null)
//
//        val inputStream = attachId.map { proxy.openAttachment(it) }
//        val zipToReturn = if (inputStream.size == 1) {
//            inputStream.single()
//        } else {
//            combineZips(inputStream, name)
//        }
//        return ResponseEntity.ok().header(
//                HttpHeaders.CONTENT_DISPOSITION,
//                "attachment; filename=\"$name.zip\"").body(InputStreamResource(zipToReturn))
//    }
//
//    private fun combineZips(inputStreams: List<InputStream>, filename: String): InputStream {
//        val zipName = "$filename-${UUID.randomUUID()}.zip"
//        FileOutputStream(zipName).use { fileOutputStream ->
//            ZipOutputStream(fileOutputStream).use { zipOutputStream ->
//                inputStreams.forEachIndexed { index, inputStream ->
//                    val zipEntry = ZipEntry("$filename-$index.zip")
//                    zipOutputStream.putNextEntry(zipEntry)
//                    inputStream.copyTo(zipOutputStream, 1024)
//                }
//            }
//        }
//        return try {
//            FileInputStream(zipName)
//        } finally {
//            Files.deleteIfExists(Paths.get(zipName))
//        }
//    }








}