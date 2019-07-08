package com.template.webserver

import com.template.flows.MyRegisterFlow
import com.template.flows.MyUpdateFlow
import com.template.flows.MyUpdateVerifyFlow
import com.template.flows.MyVerifyFlow
import com.template.model.*
import com.template.states.MyState
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private const val CONTROLLER_NAME = "config.controller.name"

@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class MyAccountController(
        private val rpc: NodeRPCConnection
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    /**
     * Return all UserAccountState
     */
    @GetMapping(value = "/my/user", produces = ["application/json"])
    private fun getUserAccountStates(): ResponseEntity<Map<String, Any>> {
        val (status, result) = try {
            val requestStateRef = rpc.proxy.vaultQueryBy<MyState>().states
            val requestStates = requestStateRef.map { it.state.data }
            val list = requestStates.map {
                MyModel(
                        registered = it.registered,
                        verify = it.verify,
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

    /**
     * REGISTER - UserAccountRegisterFlow
     */

    @PostMapping(value = "/my/user/create", produces = ["application/json"])
    private fun myModelRegister(@RequestBody myModelRegiser: MyModelRegister) : ResponseEntity<Map<String,Any>> {

        val (status, result) = try {
            val user = MyModelRegister(
                    registered = myModelRegiser.registered

            )
            proxy.startFlowDynamic(
                    MyRegisterFlow::class.java,
                    user.registered
            )



            HttpStatus.CREATED to user
        }catch (e: Exception){
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status==HttpStatus.CREATED){
            "message" to "Successful in creating ContractState of type UserAccountState"}
        else{ "message" to "Failed to create ContractState of type UserAccountState"}
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }

    /**
     * VERIFY - UserAccountVerifyFlow
     */

    @PostMapping(value = "/my/user/verify", produces = ["application/json"])
    private fun myModelVerify(@RequestBody myModelVerify: MyModelVerify) : ResponseEntity<Map<String,Any>> {

        val (status, result) = try {
            val user = MyModelVerify(
                    counterParty = myModelVerify.counterParty,
                    linearId = myModelVerify.linearId

            )
            proxy.startFlowDynamic(
                    MyVerifyFlow::class.java,
                    user.counterParty,
                    user.linearId
            )

            HttpStatus.CREATED to user
        }catch (e: Exception){
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status==HttpStatus.CREATED){
            "message" to "Successful in creating ContractState of type UserAccountState"}
        else{ "message" to "Failed to create ContractState of type UserAccountState"}
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }

    /**
     * Update - UserAccountUpdateFlow
     */

    @PutMapping (value = "my/user/update", produces = ["application/json"])
    private fun myModelUpdate (@RequestBody myModelUpdate: MyModelUpdate) : ResponseEntity <Map<String,Any>> {

        val (status, result) = try {
            val user = MyModelUpdate(
                    registered = myModelUpdate.registered,
                    counterParty = myModelUpdate.counterParty,
                    linearId = myModelUpdate.linearId
            )

             proxy.startFlowDynamic(
                    MyUpdateFlow::class.java,
                    user.registered,
                    user.counterParty,
                    user.linearId
            )

            HttpStatus.CREATED to user
        } catch (e: Exception){
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status==HttpStatus.CREATED){
            "message" to "Successful in creating ContractState of type UserAccountState"}
        else{ "message" to "Failed to create ContractState of type UserAccountState"}
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }
    /**
     * UpdateRegister - UserAccountUpdateRegisterFlow
     */

    @PutMapping (value = "my/user/updateRegister", produces = ["application/json"])
    private fun myModelUpdateRegister (@RequestBody myModelUpdateRegister: MyModelUpdateRegister) : ResponseEntity <Map<String,Any>>{

        val (status, result) = try {
            val user = MyModelUpdateRegister (
                    registered = myModelUpdateRegister.registered,
                    counterParty = myModelUpdateRegister.counterParty,
                    linearId = myModelUpdateRegister.linearId
            )

            proxy.startFlowDynamic(
                    MyUpdateVerifyFlow::class.java,
                    user.registered,
                    user.counterParty,
                    user.linearId
            )

            HttpStatus.CREATED to user
        } catch (e: Exception){
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status==HttpStatus.CREATED){
            "message" to "Successful in creating ContractState of type UserAccountState"}
        else{ "message" to "Failed to create ContractState of type UserAccountState"}
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }






}