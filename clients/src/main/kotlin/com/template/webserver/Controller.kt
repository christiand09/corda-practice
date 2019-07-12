package com.template.webserver

import com.template.flows.UserContentsFlows.*
import com.template.models.*
import com.template.states.UserState
import com.template.webserver.utilities.FlowHandlerCompletion
import net.corda.core.crypto.SecureHash
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import net.corda.core.messaging.vaultQueryBy
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.net.URI

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/register") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection, val flowHandlerCompletion : FlowHandlerCompletion) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

//  @GetMapping(value = "/templateendpoint", produces = arrayOf("text/plain"))
//    private fun templateendpoint(): String {
//        return "Define an endpoint here."
//    }



    @GetMapping(value = "/states/user", produces = arrayOf("application/json"))
    private fun getUserState() : ResponseEntity<Map<String, Any>> {
        val(status, result) = try {
            val userStateRef = proxy.vaultQueryBy<UserState>().states
            val userState = userStateRef.map { it.state.data }
            val list = userState.map {

                UserModel(name= it.name,verify = it.verify,linearId = it.linearId)
            }
            HttpStatus.CREATED to list
        }catch (e: Exception){HttpStatus.BAD_REQUEST to "No data"}
        val stat = "status" to status

        val mess = if (status==HttpStatus.CREATED){
            "message" to "Successful"}
        else{ "message" to "Failed"}
        val res = "result" to result

        return ResponseEntity.status(status).body(mapOf(stat,mess,res))

    }
    @PostMapping(value = "/states/user/create", produces = arrayOf("application/json"))

    private fun createUser(@RequestBody createUserAccount: createUserModel) : ResponseEntity<Map<String,Any>> {
        val (status, result) = try {
            val createdUser = createUserModel(createUserAccount.name)

            val flowReturn = proxy.startFlowDynamic(

                    UserFlow::class.java,
                    createdUser.name

            )

            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to createUserAccount
//            val userStateRef = proxy.vaultQueryBy<UserState>().states.last()
//            val userStateData = userStateRef.state.data
//            val list = UserModel(name = userStateData.name, verify = userStateData.verify, linearId = userStateData.linearId.toString())


        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "$e"
        }
        val stat = "status" to status.value()

        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful"
        } else {
            "message" to "Failed"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }



    @PostMapping(value = "/states/user/verify", produces = arrayOf("application/json"))

    private fun verifyUser(@RequestBody verifyUserAccount: verifyUserModel) : ResponseEntity<Map<String,Any>> {
        val (status, result) = try {
            val verifiedUser = verifyUserModel(receiver = verifyUserAccount.receiver,linearId = verifyUserAccount.linearId)

            val flowReturn = proxy.startFlowDynamic(

                    VerifyFlow::class.java,
                    verifiedUser.receiver,
                    verifiedUser.linearId
            )

            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to verifyUserAccount
//            val userStateRef = proxy.vaultQueryBy<UserState>().states.last()
//            val userStateData = userStateRef.state.data
//            val list = UserModel(name = userStateData.name, verify = userStateData.verify, linearId = userStateData.linearId.toString())

        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "$e"
        }
        val stat = "status" to status.value()

        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful"
        } else {
            "message" to "Failed"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }
    @PutMapping(value = "/states/user/update", produces = arrayOf("application/json"))

    private fun updateUser(@RequestBody updateUserAccount: updateUserModel) : ResponseEntity<Map<String,Any>> {
        val (status, result) = try {
            val updatedUser = updateUserModel(name = updateUserAccount.name, receiver = updateUserAccount.receiver,linearId = updateUserAccount.linearId)

            val flowReturn = proxy.startFlowDynamic(

                    UpdateFlow::class.java,
                    updatedUser.name,
                    updatedUser.receiver,
                    updatedUser.linearId
            )

            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to updateUserAccount
//            val userStateRef = proxy.vaultQueryBy<UserState>().states.last()
//            val userStateData = userStateRef.state.data
//            val list = UserModel(name = userStateData.name, verify = userStateData.verify, linearId = userStateData.linearId.toString())

        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "$e"
        }
        val stat = "status" to status.value()

        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful"
        } else {
            "message" to "Failed"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }
    @PutMapping(value = "/states/user/updateregister", produces = arrayOf("application/json"))

    private fun updateRegisterUser(@RequestBody updateRegisterUserAccount: updateRegisterUserModel) : ResponseEntity<Map<String,Any>> {
        val (status, result) = try {
            val updatedRegisterUser = updateUserModel(name = updateRegisterUserAccount.name, receiver = updateRegisterUserAccount.receiver,linearId = updateRegisterUserAccount.linearId)

            val flowReturn = proxy.startFlowDynamic(

                    UpdateRegisterFlow::class.java,
                    updatedRegisterUser.name,
                    updatedRegisterUser.receiver,
                    updatedRegisterUser.linearId
            )

            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to updateRegisterUserAccount
//            val userStateRef = proxy.vaultQueryBy<UserState>().states.last()
//            val userStateData = userStateRef.state.data
//            val list = UserModel(name = userStateData.name, verify = userStateData.verify, linearId = userStateData.linearId.toString())

        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "$e"
        }
        val stat = "status" to status.value()

        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful"
        } else {
            "message" to "Failed"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }

     /****************
     * AttachmentFlow*
     ****************/
     @PostMapping(value = "/attachments/uploadreceiver", produces = arrayOf("application/json"))

     private fun attachmentsModel(@RequestBody uploadAttachments: uploadAttachmentModel) : ResponseEntity<Map<String,Any>> {
         val (status, result) = try {

             val uploadedAttachment = uploadAttachmentModel(uploadAttachments.receiver,uploadAttachments.hash)
             val flowReturn = proxy.startFlowDynamic(

                     AttachmentsFlow::class.java,
                     uploadedAttachment.receiver,
                     uploadedAttachment.hash
             )

             flowHandlerCompletion.flowHandlerCompletion(flowReturn)
             HttpStatus.CREATED to uploadAttachments


         } catch (e: Exception) {
             HttpStatus.BAD_REQUEST to "$e"
         }
         val stat = "status" to status.value()

         val mess = if (status == HttpStatus.CREATED) {
             "message" to "Successful"
         } else {
             "message" to "Failed"
         }
         val res = "result" to result
         return ResponseEntity.status(status).body(mapOf(stat, mess, res))
     }











     /************
     * UPLOADING *
     ************/

     @PostMapping("/attachments/upload")
     fun upload(@RequestParam file: MultipartFile, @RequestParam uploader: String): ResponseEntity<String> {
         val filename = file.originalFilename
         val hash: SecureHash =
             proxy.uploadAttachmentWithMetadata(

                     jar = file.inputStream,
                     uploader = uploader,
                     filename = filename!!
             )

         return ResponseEntity.created(URI.create("attachments/$hash")).body("Attachment uploaded with hash - $hash")
     }


    /************
     ** Download**
     ************/

    @GetMapping("/attachments/{hash}")
     fun downloadByHash(@PathVariable hash: String): ResponseEntity<Resource> {
         val inputStream = InputStreamResource(proxy.openAttachment(SecureHash.parse(hash)))
         return ResponseEntity.ok().header(
                 HttpHeaders.CONTENT_DISPOSITION,
                 "attachment; filename=\"$hash.zip\""
         ).body(inputStream)
     }







}