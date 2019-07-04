//package com.template.webserver.Controllerrr
//
//import com.template.states.UserState
//
//import com.template.webserver.NodeRPCConnection
//
//import net.corda.core.messaging.vaultQueryBy
//
//import org.slf4j.LoggerFactory
//
//import org.springframework.beans.factory.annotation.Value
//
//import org.springframework.web.bind.annotation.GetMapping
//
//import org.springframework.web.bind.annotation.RequestMapping
//
//import org.springframework.web.bind.annotation.RestController
//
//
//
//
//
//private const val CONTROLLER_NAME = "config.controller.name"
//
///**
//
// * Define your API endpoints here.
//
// */
//
//@RestController
//
//@RequestMapping("/kyc") // The paths for HTTP requests are relative to this base path.
//
//class Controller(
//
//        private val rpc: NodeRPCConnection, @Value("\${$CONTROLLER_NAME}") private val controllerName: String) {
//
//
//
//    companion object {
//
//        private val logger = LoggerFactory.getLogger(RestController::class.java)
//
//    }
//
//    private val myName = rpc.proxy.nodeInfo().legalIdentities.first().name
//
//    private val proxy = rpc.proxy
//
//
//
//    /** Maps a UserState to a JSON object. */ /**Get all data Registered in states using API*/
//
//    private fun UserState.toJson(): Map<String, Any> {
//
//
//
//        return mapOf(
//
//
//
//                "ownParty" to ownParty.toString(),
//
//                "name" to name,
//
//                "age" to age,
//
//                "address" to address,
//
//                "birthDate" to birthDate,
//
//                "status" to status,
//
//                "religion" to religion,
//
//                "participants" to participants.toString(),
//
//                "linearId" to linearId,
//
//                "verified" to isVerified,
//
//                "notary" to proxy.notaryIdentities().toString()
//
//
//
//
//
//        )
//
//    }
//
//
//
//    /** Returns a list of existing state's. */ /**Get all data Registered in states using API*/
//
//    @GetMapping(value = "/states", produces = arrayOf("application/json"))
//
//    private fun getStates(): List<Map<String, Any>> {
//
//        val StateAndRefs = rpc.proxy.vaultQueryBy<UserState>().states
//
//        val States = StateAndRefs.map { it.state.data }
//
//        return States.map { it.toJson() }
//
//    }
//
//}