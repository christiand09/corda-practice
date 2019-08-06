package com.template.webserver


import com.template.flows.clientSampleRegister.ClientRegisterFlow
import com.template.flows.clientSampleRegister.ClientUpdateFlow
import com.template.flows.clientSampleRegister.ClientUpdateRegisterFlow
import com.template.flows.clientSampleRegister.ClientVerifyFlow
import com.template.model.*
import com.template.states.ClientState
import com.template.webserver.utilities.FlowHandlerCompletion
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private const val CONTROLLER_NAME = "config.controller.name"
//@Value("\${$CONTROLLER_NAME}") private val controllerName: String
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class UserAccountController(
        private val rpc: NodeRPCConnection,
        private val flowHandlerCompletion : FlowHandlerCompletion<Any?>
) {
        companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }
        private val proxy = rpc.proxy


    /**
     * Return all UserAccountState
     */
    @GetMapping(value = ["/states/user"], produces = ["application/json"])
    private fun getUserAccountStates() : ResponseEntity<Map<String, Any>> {
        val (status, result ) = try {
            val requestStateRef = rpc.proxy.vaultQueryBy<ClientState>().states
            val requestStates = requestStateRef.map { it.state.data }
            val list = requestStates.map {
                ClientUserModel(
                        calls = it.calls,
                        verify = it.verify,
                        linearId = it.linearId

                )
            }
            HttpStatus.CREATED to list
        }catch( e: Exception){
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status==HttpStatus.CREATED){
            "message" to "Successful in getting ContractState of type UserAccountState"}
        else{ "message" to "Failed to get ContractState of type UserAccountState"}
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }


    /**
     * REGISTER - UserAccountRegisterFlow
     */

    @PostMapping(value = ["/states/user/create"], produces = ["application/json"])
    private fun createUserAccount(@RequestBody createUserAccount: CreateUserAccount) : ResponseEntity<Map<String,Any>> {

        val (status, result) = try {
            val user = CreateUserAccount(
                    calls = createUserAccount.calls

            )
            val flowReturn= proxy.startFlowDynamic(
                    ClientRegisterFlow::class.java,
                    user.calls
            )


            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to createUserAccount
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

    @PostMapping(value = ["/states/user/verify"], produces = ["application/json"])
    private fun verifyUserAccount(@RequestBody verifyUserAccount: VerifyUserAccount) : ResponseEntity<Map<String,Any>> {

        val (status, result) = try {
            val user = VerifyUserAccount(
                    counterparty = verifyUserAccount.counterparty,
                    linearId = verifyUserAccount.linearId

            )
            val flowReturn = proxy.startFlowDynamic(
                    ClientVerifyFlow::class.java,
                    user.counterparty,
                    user.linearId
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to verifyUserAccount
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

    @PutMapping (value = ["states/user/update"], produces = ["application/json"])
    private fun updateUserAccount (@RequestBody updateUserAccount: UpdateUserAccount) : ResponseEntity <Map<String,Any>> {

        val (status, result) = try {
            val user = UpdateUserAccount(
                    calls = updateUserAccount.calls,
                    counterparty = updateUserAccount.counterparty,
                    linearId = updateUserAccount.linearId
            )

            val flowReturn= proxy.startFlowDynamic(
                    ClientUpdateFlow::class.java,
                    user.calls,
                    user.counterparty,
                    user.linearId
            )

            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to updateUserAccount
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

    @PutMapping (value = ["states/user/updateRegister"], produces = ["application/json"])
    private fun updateRegisterAccount (@RequestBody updateRegisterAccount: UpdateRegisterAccount) : ResponseEntity <Map<String,Any>>{

        val (status, result) = try {
            val user = UpdateRegisterAccount (
                    calls = updateRegisterAccount.calls,
                    counterparty = updateRegisterAccount.counterparty,
                    linearId = updateRegisterAccount.linearId
            )

            val flowReturn = proxy.startFlowDynamic(
                    ClientUpdateRegisterFlow::class.java,
                    user.calls,
                    user.counterparty,
                    user.linearId
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to updateRegisterAccount
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