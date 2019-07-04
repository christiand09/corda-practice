package com.template.webserver.Controllerrr

import com.template.flows.RegisterUserFlow
import com.template.flows.VerifyUserFlow
import com.template.flows.UpdateUserFlow
import com.template.flows.UpdateRegisterUserFlow
import com.template.states.MyState
import com.template.webserver.NodeRPCConnection
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
//import com.template.flow.Encryption.md5
import com.template.models.*
import com.template.states.formSet
import net.corda.core.contracts.StateAndRef
import javax.servlet.http.HttpServletRequest
import com.template.webserver.utilities.FlowHandlerCompletion

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
            "message" to "Successful in getting ContractState of type UserAccountState"}
        else{ "message" to "Failed to get ContractState of type UserAccountState"}
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }
    /**

     * REGISTER - RegisterUserFlow

     */
    @PostMapping(value = "/states/user/create", produces = arrayOf("application/json"))
    private fun createUser(@RequestBody createUserAccount: createUserAccount) : ResponseEntity<Map<String,Any>> {
        val (status, result) = try {
            val user = createUserAccount(
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
            "message" to "Successful in creating ContractState of type UserAccountState"}
        else{ "message" to "Failed to create ContractState of type UserAccountState"}
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
//            val out = registerFlow.use { it.returnValue.getOrThrow() }
//            val userStateRef = proxy.vaultQueryBy<MyState>().states.last()
//            val userStateData = userStateRef.state.data
//            val list = userAccountModel(
//                    formSet = userStateData.formSet
//            )
            HttpStatus.CREATED to verifyUserAccount
        }catch (e: Exception){
            HttpStatus.BAD_REQUEST to "$e"
        }
        val stat = "status" to status
        val mess = if (status==HttpStatus.CREATED){
            "message" to "Successful in creating ContractState of type UserAccountState"}
        else{ "message" to "Failed to create ContractState of type UserAccountState"}
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }

}

