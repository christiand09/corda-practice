package com.template.webserver

import com.template.model.AttachmentModel
import com.template.states.AttachmentState
import com.template.webserver.utilities.FlowHandlerCompletion
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private const val CONTROLLER_NAME = "config.controller.name"
//@Value("\${$CONTROLLER_NAME}") private val controllerName: String
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class AttachmentController(
        private val rpc: NodeRPCConnection,
        private val flowHandlerCompletion : FlowHandlerCompletion
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy


    /**
     * Return all UserAccountState
     */

    @GetMapping(value = "/attachment/user", produces = ["application/json"])
    private fun getUserAccountStates(): ResponseEntity<Map<String, Any>> {
        val (status, result) = try {
            val requestStateRef = rpc.proxy.vaultQueryBy<AttachmentState>().states
            val requestStates = requestStateRef.map { it.state.data }
            val list = requestStates.map {
                AttachmentModel(
                        hash = it.hash,
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
}