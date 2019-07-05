package com.template.webserver

import com.template.flows.RegisterFlow
import com.template.flows.UpdateFlow
import com.template.flows.UpdateRegisterFlow
import com.template.flows.VerifyFlow
import com.template.models.*

import com.template.states.ClientInfo
import com.template.states.RegisterState
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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
}