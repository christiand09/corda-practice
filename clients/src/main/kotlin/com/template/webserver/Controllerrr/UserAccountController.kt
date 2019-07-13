package com.template.webserver.Controllerrr

import com.template.flows.DataFlows.RegisterUserFlow
import com.template.flows.DataFlows.VerifyUserFlow
import com.template.flows.DataFlows.UpdateUserFlow
//import com.template.flows.otherflows.UpdateRegisterUserFlow
import com.template.states.MyState
import com.template.webserver.NodeRPCConnection
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
//import com.template.flow.Encryption.md5
import com.template.models.*
import com.template.webserver.utilities.FlowHandlerCompletion
import net.corda.core.crypto.SecureHash
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.web.multipart.MultipartFile
//import java.io.InputStream
import java.net.URI

private const val CONTROLLER_NAME = "config.controller.name"

//@Value("\${$CONTROLLER_NAME}") private val controllerName: String

@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class UserAccountController(
        private val rpc: NodeRPCConnection,
        val flowHandlerCompletion :FlowHandlerCompletion
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }
    private val proxy = rpc.proxy
    /**

     * Return all UserAccountState

     */
    @GetMapping(value = "/states/user", produces = arrayOf("application/json"))
    private fun getMyState() : ResponseEntity<Map<String,Any>>{
        val (status, result ) = try {
            val requestStateRef = rpc.proxy.vaultQueryBy<MyState>().states
            val requestStates = requestStateRef.map { it.state.data }
            val list = requestStates.map {
                userAccountModel(
                        formSet = it.formSet,
                        approvals = it.approvals,
                        linearId = it.linearId
                )
            }
            HttpStatus.CREATED to list
        }catch( e: Exception){
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status==HttpStatus.CREATED){
            "message" to "Successful in getting all states"}
        else{ "message" to "Failed to get all states"}
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }
    /**

     * REGISTER - RegisterUserFlow

     */
        @PostMapping(value = "/states/user/create", produces = arrayOf("application/json"))
    private fun createUser(@RequestBody createUserAccount: createUserModel) : ResponseEntity<Map<String,Any>> {
        val (status, result) = try {
            val user = createUserModel(
                    formSet = createUserAccount.formSet
            )
           val flowReturn = proxy.startFlowDynamic(
                    RegisterUserFlow::class.java,
                    user.formSet
            )
//            val out = registerFlow.use { it.returnValue.getOrThrow() }
//            val userStateRef = proxy.vaultQueryBy<MyState>().states.last()
//            val userStateData = userStateRef.state.data
//            val list = userAccountModel(
//                    formSet = userStateData.formSet
//            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to createUserAccount
        }catch (e: Exception){
            HttpStatus.BAD_REQUEST to "$e"
        }
        val stat = "status" to status
        val mess = if (status==HttpStatus.CREATED){
            "message" to "Successful in registering a State"}
        else{ "message" to "Failed to register a State"}
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }
    /**

     * VERIFY - VerifyUserFlow

     */
    @PostMapping(value = "/states/user/verify", produces = arrayOf("application/json"))
    private fun verifyUser(@RequestBody verifyUserAccount: verifyUserModel) : ResponseEntity<Map<String,Any>> {
        val (status, result) = try {
            val user = verifyUserModel(
                    receiver = verifyUserAccount.receiver,
                    linearId = verifyUserAccount.linearId
            )
            val flowReturn  = proxy.startFlowDynamic(
                    VerifyUserFlow::class.java,
                    user.receiver,
                    user.linearId
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to verifyUserAccount
        }catch (e: Exception){
            HttpStatus.BAD_REQUEST to "$e"
        }
        val stat = "status" to status
        val mess = if (status==HttpStatus.CREATED){
            "message" to "Successful in verifying a user"}
        else{ "message" to "Failed to verify a user"}
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }
    /**

     * UPDATE - UpdateUserFlow

     */
    @PostMapping(value = "/states/user/update", produces = arrayOf("application/json"))
    private fun updateUser(@RequestBody updateUserAccount: updateUserModel) : ResponseEntity<Map<String,Any>> {
        val (status, result) = try {
            val user = updateRegisterUserModel(
                    formSet = updateUserAccount.formSet,
                    receiver = updateUserAccount.receiver,
                    linearId = updateUserAccount.linearId
            )
            val flowReturn  = proxy.startFlowDynamic(
                    UpdateUserFlow::class.java,
                    user.formSet,
                    user.receiver,
                    user.linearId
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to updateUserAccount
        }catch (e: Exception){
            HttpStatus.BAD_REQUEST to "$e"
        }
        val stat = "status" to status
        val mess = if (status==HttpStatus.CREATED){
            "message" to "Successful in Updating State"}
        else{ "message" to "Failed to Update State"}
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }

    /**

     * UPDATE_REGISTER - UpdateRegisterUserFlow

     */

//    @PostMapping(value = "/states/user/updateregister", produces = arrayOf("application/json"))
//    private fun updateRegisterUser(@RequestBody updateRegisterUserAccount: updateRegisterUserModel) : ResponseEntity<Map<String,Any>> {
//        val (status, result) = try {
//            val user = updateRegisterUserModel(
//                    formSet = updateRegisterUserAccount.formSet,
//                    receiver = updateRegisterUserAccount.receiver,
//                    linearId = updateRegisterUserAccount.linearId
//            )
//            val flowReturn  = proxy.startFlowDynamic(
//                    UpdateRegisterUserFlow::class.java,
//                    user.formSet,
//                    user.receiver,
//                    user.linearId
//            )
//            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
//            HttpStatus.CREATED to updateRegisterUserAccount
//        }catch (e: Exception){
//            HttpStatus.BAD_REQUEST to "$e"
//        }
//        val stat = "status" to status
//        val mess = if (status==HttpStatus.CREATED){
//            "message" to "Successful in Updating a State and Registered it on Counter party"}
//        else{ "message" to "Failed to Update a State and Register on Counter party"}
//        val res = "result" to result
//        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
//    }
    /**

     * ATTACHMENT - Upload

     */
    @PostMapping (value="/attachment")
    fun upload(@RequestParam file: MultipartFile, @RequestParam uploader: String): ResponseEntity<String> {
        val filename = file.originalFilename
//        require(filename != null) { "File name must be set" }
        val hash: SecureHash =
//                if (!(file.contentType == "zip" || file.contentType == "jar")) {
//            uploadZip(file.inputStream, uploader, filename!!)
//        } else {
            proxy.uploadAttachmentWithMetadata(
                    jar = file.inputStream,
                    uploader = uploader,
                    filename = filename!!
            )
//        }
        return ResponseEntity.created(URI.create("attachments/$hash")).body("Attachment uploaded with hash - $hash")
    }

//    private fun uploadZip(inputStream: InputStream, uploader: String, filename: String): AttachmentId {
//        val zipName = "$filename-${UUID.randomUUID()}.zip"
//        FileOutputStream(zipName).use { fileOutputStream ->
//            ZipOutputStream(fileOutputStream).use { zipOutputStream ->
//                val zipEntry = ZipEntry(filename)
//                zipOutputStream.putNextEntry(zipEntry)
//                inputStream.copyTo(zipOutputStream, 1024)
//            }
//        }
//        return FileInputStream(zipName).use { fileInputStream ->
//            val hash = proxy.uploadAttachmentWithMetadata(
//                    jar = fileInputStream,
//                    uploader = uploader,
//                    filename = filename
//            )
//            Files.deleteIfExists(Paths.get(zipName))
//            hash
//        }
//    }
    /**

     * ATTACHMENT - Download

     */
    @GetMapping("attachmentDownload/{hash}")
    fun downloadByHash(@PathVariable hash: String): ResponseEntity<InputStreamResource>? {
        val inputStream = InputStreamResource(proxy.openAttachment(SecureHash.parse(hash)))
        return ResponseEntity.ok().header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"$hash.zip\""
        ).body(inputStream)
    }

}

