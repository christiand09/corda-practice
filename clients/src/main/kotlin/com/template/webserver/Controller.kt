package com.template.webserver

import com.template.flows.UserFlow.*
import com.template.models.*
import com.template.states.AttachmentState
import com.template.states.UserState
import com.template.webserver.utilities.FlowHandlerCompletion
import net.corda.core.crypto.SecureHash
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.AttachmentId
import net.corda.core.node.services.vault.AttachmentQueryCriteria
import net.corda.core.node.services.vault.Builder
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.nio.file.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/register") // The paths for HTTP requests are relative to this base path.
class Controller(private val rpc: NodeRPCConnection, val flowhandler:FlowHandlerCompletion) {

    private val proxy = rpc.proxy

    @GetMapping(value = "/states/getuser", produces = arrayOf("application/json"))
    private fun getUsers(): ResponseEntity<Map<String,Any>>
    {
        val (status, result ) = try {
        val userStateRef = proxy.vaultQueryBy<UserState>().states
        val userStates = userStateRef.map { it.state.data }
        val list = userStates.map {
            UserModel(
                    name = it.name,
                    verify = it.verify,
                    linearId = it.linearId)
        }
        HttpStatus.CREATED to list
    }
    catch(e: Exception)
    {
        HttpStatus.BAD_REQUEST to "No data"
    }
    val stat = "status" to status
    val mess = if (status==HttpStatus.CREATED){
        "message" to "Successful in getting ContractState of type UserState"}
    else{ "message" to "Failed to get ContractState of type UserState"}
    val res = "result" to result
    return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }

    @PostMapping("/states/registerUser",produces = ["application/json"])
    private fun registerClient(@RequestBody addUser : addUsers): ResponseEntity<Map<String,Any>>
    {
        val (status,result) = try {
                val add = addUsers(name = addUser.name)

               val flowReturn =  proxy.startFlowDynamic(
                        UserRegisterFlow::class.java,
                        add.name
                )
            flowhandler.flowHandlerCompletion(flowReturn)
                HttpStatus.CREATED to addUser

        } catch (e:Exception)
        {
            HttpStatus.BAD_REQUEST to "Data creation failed"
        }
        val stat = "status" to status.value()
        val mess = if (status==HttpStatus.CREATED){
            "message" to "Successful in creating ContractState of type KYCState"}
        else{ "message" to "Failed to create ContractState of type KYCState"}
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }
    @PostMapping( "states/verify", produces = ["application/json"])
    private fun verify(@RequestBody verifyAccount: verifyUser) : ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val verify = verifyUser(
                    linearId =  verifyAccount.linearId,
                    counterparty = verifyAccount.counterparty
            )
            val flowReturn = proxy.startFlowDynamic(
                    UserVerifyFlow::class.java,
                    verify.linearId,
                    verify.counterparty
            )
            flowhandler.flowHandlerCompletion(flowReturn)
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
    @PostMapping("/states/update",produces = ["application/json"])
    private fun updateUser(@RequestBody update:updateUser): ResponseEntity<Map<String,Any>>
    {
        val (status,result) = try {
            val updatedata = updateUser(
                    name = update.name,
                    counterparty = update.counterparty,
                    linearId = update.linearId

            )
         val flowReturn = proxy.startFlowDynamic(
                 UserUpdateFlow::class.java,
                 updatedata.name,
                 updatedata.counterparty,
                 updatedata.linearId
         )
            flowhandler.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to update
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
    @PostMapping("/states/updateregister",produces = ["application/json"])
    private fun updateregisterUser(@RequestBody updateregister:updateandregister): ResponseEntity<Map<String,Any>>
    {
        val (status,result) = try {
            val updatedata = updateandregister(
                    name = updateregister.name,
                    counterparty = updateregister.counterparty,
                    linearId = updateregister.linearId

            )
            val flowReturn = proxy.startFlowDynamic(
                    UpdateandRegisterFlow::class.java,
                    updatedata.name,
                    updatedata.counterparty,
                    updatedata.linearId
            )
            flowhandler.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to updateregister
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
    @PostMapping("states/gethashfile",produces = ["application/json"])
    fun gethash(@RequestParam file: MultipartFile, @RequestParam uploader:String): ResponseEntity<String> {
            val filename = file.originalFilename
            require(filename != null) { "Filename must be set" }
        val hash: SecureHash =
            proxy.uploadAttachmentWithMetadata(
                    jar = file.inputStream,
                    uploader = uploader,
                    filename = filename!!
            )
        return ResponseEntity.created(URI.create("attachments/$hash")).body("attachment has hash of: - $hash")
    }
    @PostMapping("states/uploadattachments", produces = ["application/json"])
    fun upload(@RequestBody uploadfile: uploadfileState): ResponseEntity<Map<String,Any>> {
        val (status, result) = try {

            val uploadFile = uploadfileState(hash = uploadfile.hash, counterparty = uploadfile.counterparty)
            val flowResult = proxy.startFlowDynamic(
                    UploadAttachmentFlow::class.java,
                    uploadFile.hash,
                    uploadFile.counterparty
            )
            flowhandler.flowHandlerCompletion(flowResult)
            HttpStatus.CREATED to uploadfile
        }
        catch (e:Exception)
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
    @GetMapping("states/downloadattachments",produces = ["application/json"])
    fun downloadbyName(@RequestParam name:String): ResponseEntity<InputStreamResource>
    {
        val id: List<AttachmentId> = proxy.queryAttachments(AttachmentQueryCriteria.AttachmentsQueryCriteria(filenameCondition =
        Builder.equal(name)),null)
        val input = id.map { proxy.openAttachment(it) }
        val zipToReturn = if(input.size == 1 )
        {
            input.single()
        }
        else
        {
            combineZips(input,name)
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"$name.zip\"")
                .body(InputStreamResource(zipToReturn))
    }
    private fun combineZips(inputStreams:List<InputStream>,filename:String):InputStream
    {
        val zipName = "$filename-${UUID.randomUUID()}.zip"
        FileOutputStream(zipName).use { fileOututStream ->
            ZipOutputStream(fileOututStream).use { zipOutputStream
                ->
                inputStreams.forEachIndexed { index, inputStream ->
                    val zipEntry = ZipEntry("$filename-$index.zip")
                    zipOutputStream.putNextEntry(zipEntry)
                    inputStream.copyTo(zipOutputStream, 1024)
                }
            }
        }
        return try {
            FileInputStream(zipName)
        }
        finally {
            Files.deleteIfExists(Paths.get(zipName))
        }

    }
    @GetMapping(value = "/states/getattachments", produces = ["application/json"])
    private fun getAttachments(): ResponseEntity<Map<String,Any>>
    {
        val (status, result ) = try {
            val attachStateRef = rpc.proxy.vaultQueryBy<AttachmentState>().states
            val attachStates = attachStateRef.map { it.state.data }
            val attachlist = attachStates.map {
                AttachmentState(
                        hash = it.hash,
                        senderParty = it.senderParty,
                        receiverParty = it.receiverParty,
                        linearId = it.linearId)
            }
            HttpStatus.CREATED to attachlist
        }
        catch(e: Exception)
        {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status==HttpStatus.CREATED){
            "message" to "Successful in getting ContractState of type UserState"}
        else{ "message" to "Failed to get ContractState of type UserState"}
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }

}