package com.template.webserver

import com.template.flows.UpdateandRegisterFlow
import com.template.flows.UserRegisterFlow
import com.template.flows.UserUpdateFlow
import com.template.flows.UserVerifyFlow
import com.template.models.*
import com.template.states.UserState
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.template.webserver.utilities.FlowHandlerCompletion


/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/register") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection, val flowhandler:FlowHandlerCompletion) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

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


}