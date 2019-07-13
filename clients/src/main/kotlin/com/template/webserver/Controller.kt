package com.template.webserver


import com.template.flows.flows.*
import com.template.models.*
import com.template.states.AttachmentState
import com.template.states.RegisterState
import com.template.webserver.utilities.FlowHandlerCompletion
import net.corda.core.crypto.SecureHash
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.net.URI

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("register") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection, private val flowHandlerCompletion :FlowHandlerCompletion ) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    /**
     * Return all RegisterState*/
    @GetMapping(value = "states/all", produces = ["application/json"])
    private fun getRegisterStates(): ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val infoStateRef = proxy.vaultQueryBy<RegisterState>().states
            val infoStates = infoStateRef.map { it.state.data }
            val list = infoStates.map {
                UserModel(
                        name = it.name,
                        approved = it.approved,
                        linearId = it.linearId
                )
            }
            HttpStatus.CREATED to list
        }
        catch (e: Exception)
        {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED)
        {
            "message" to "Successful"
        }
        else
        {
            "message" to "Failed"
        }

        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }


    /**
     * REGISTER - RegisterFlow
     */
    @PostMapping(value = "states/user/register", produces = ["application/json"])
    private fun registerUser(@RequestBody registerAccount: UserRegisterModel) : ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val user = UserRegisterModel(
                    name = registerAccount.name
            )
            val flowReturn = proxy.startFlowDynamic(
                    RegisterFlow::class.java,
                    user.name
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to registerAccount
        }
        catch (e: Exception)
        {
            HttpStatus.BAD_REQUEST to e
        }

        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED)
        {
            "mesasge" to "Successful"
        }
        else
        {
            "message" to "Failed"
        }

        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }


    /**
     * VERIFY - VerifyFlow
     */
    @PostMapping(value = "states/user/verify", produces = ["application/json"])
    private fun verifyUser(@RequestBody verifyAccount: UserVerifyModel) : ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val verify = UserVerifyModel(
                counterParty = verifyAccount.counterParty,
                linearId = verifyAccount.linearId
            )
            val flowReturn = proxy.startFlowDynamic(
                    VerifyFlow::class.java,
                    verify.counterParty,
                    verify.linearId
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to verifyAccount
        }
        catch (e: Exception)
        {
            HttpStatus.BAD_REQUEST to e
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED)
        {
            "mesasge" to "Successful"
        }
        else
        {
            "message" to "Failed"
        }

        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }

    /**
     * UPDATE - UpdateFlow
     */
    @PutMapping(value = "states/user/update", produces = ["application/json"])
    private fun updateUser(@RequestBody updateAccount: UserUpdateModel) : ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val update = UserUpdateModel(
                    name = updateAccount.name,
                    counterParty = updateAccount.counterParty,
                    linearId = updateAccount.linearId
            )
            val flowReturn = proxy.startFlowDynamic(
                    UpdateFlow::class.java,
                    update.name,
                    update.counterParty,
                    update.linearId
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to updateAccount
        }
        catch (e: Exception)
        {
            HttpStatus.BAD_REQUEST to e
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED)
        {
            "mesasge" to "Successful"
        }
        else
        {
            "message" to "Failed"
        }

        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }


    /**
     * UPDATEREGISTER - UpdateRegisterFlow
     */
    @PutMapping(value = "states/user/updateregister", produces = ["application/json"])
    private fun updateAndRegisterUser(@RequestBody updateAccount: UserUpdateRegisterModel) : ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val update = UserUpdateRegisterModel(
                    name = updateAccount.name,
                    counterParty = updateAccount.counterParty,
                    linearId = updateAccount.linearId
            )
            val flowReturn = proxy.startFlowDynamic(
                    UpdateRegisterFlow::class.java,
                    update.name,
                    update.counterParty,
                    update.linearId
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to updateAccount
        }
        catch (e: Exception)
        {
            HttpStatus.BAD_REQUEST to e
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED)
        {
            "mesasge" to "Successful"
        }
        else
        {
            "message" to "Failed"
        }

        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }

    /**
     * ATTACHMENT -
     */
    @GetMapping(value = "attachments/all", produces = ["application/json"])
    private fun getAttachmentStates(): ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val infoStateRef = proxy.vaultQueryBy<AttachmentState>().states
            val infoStates = infoStateRef.map { it.state.data }
            val list = infoStates.map {
                AttachmentModel(
                        hash = it.hash,
                        linearId = it.linearId
                )
            }
            HttpStatus.CREATED to list
        }
        catch (e: Exception)
        {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED)
        {
            "message" to "Successful"
        }
        else
        {
            "message" to "Failed"
        }

        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }

    /**
     * ATTACHMENT FLOW
     */

    @PostMapping(value = "attachments/user")
    private fun saveAttachment(@RequestBody modelAttachment: AttachmentFlowModel) : ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val attachment = AttachmentFlowModel(
                    hash = modelAttachment.hash,
                    hash2 = modelAttachment.hash2,
                    hash3 = modelAttachment.hash3
            )
            val flowReturn = proxy.startFlowDynamic(
                    AttachmentFlow::class.java,
                    attachment.hash,
                    attachment.hash2,
                    attachment.hash3
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to modelAttachment
        }
        catch (e: Exception)
        {
            HttpStatus.BAD_REQUEST to e
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED)
        {
            "mesasge" to "Successful"
        }
        else
        {
            "message" to "Failed"
        }

        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }

    /**
     * UPLOAD ATTACHMENT
     */

    @PostMapping(value = "attachments/upload")
    private fun uploadAttachment(@RequestParam file: MultipartFile, @RequestParam uploader: String): ResponseEntity<String>
    {
        return try {
            val filename = file.originalFilename
            val hash: SecureHash =
                    proxy.uploadAttachmentWithMetadata(
                            jar = file.inputStream,
                            uploader = uploader,
                            filename = filename!!
                    )
            ResponseEntity.created(URI.create("attachments/$hash")).body("Attachment uploaded with hash - $hash")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body("$e")
        }
    }

    /**
     * DOWNLOAD ATTACHMENT
     */
    @GetMapping("attachment/{hash}")
    fun downloadByHash(@PathVariable hash: String): ResponseEntity<Resource> {
        val inputStream = InputStreamResource(proxy.openAttachment(SecureHash.parse(hash)))
        return ResponseEntity.ok().header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"$hash.zip\""
        ).body(inputStream)
    }
}