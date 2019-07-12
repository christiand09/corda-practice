//package com.template.Controller
//
//import com.template.states.UserState
//import com.template.webserver.NodeRPCConnection
//import net.corda.core.messaging.vaultQueryBy
//import org.slf4j.LoggerFactory
//import org.springframework.http.ResponseEntity
//import org.springframework.web.bind.annotation.GetMapping
//import org.springframework.web.bind.annotation.RequestMapping
//import org.springframework.web.bind.annotation.RestController
//import com.template.models.*
//import org.springframework.http.HttpStatus
//
//private const val CONTROLLER_NAME = "config.controller.name"
//
//@RestController
//@RequestMapping("/")
//
//class UserController(
//        private val rpc: NodeRPCConnection
//)
//{
//    companion object {
//
//        private val logger = LoggerFactory.getLogger(RestController::class.java)
//
//    }
//    private val proxy = rpc.proxy
//
//    @GetMapping(value = "/states/user", produces = arrayOf("application/json"))
//    private fun getRegisterState() : ResponseEntity<Map<String, Any>>{
//        val(status, result) = try {
//        val UserStateRef = proxy.vaultQueryBy<UserState>().states
//        val userState = UserStateRef.map { it.state.data }
//        val list = userState.map {
//
//            UserModel(name = it.name,sender = it.sender,receiver = it.receiver,verify = it.verify,linearId = it.linearId)
//        }
//        HttpStatus.CREATED to list
//    }catch (e: Exception){HttpStatus.BAD_REQUEST to "No data"}
//        val stat = "status" to status
//
//        val mess = if (status==HttpStatus.CREATED){
//
//            "message" to "Successful in getting ContractState of type KYCState"}
//
//        else{ "message" to "Failed to get ContractState of type KYCState"}
//
//        val res = "result" to result
//
//        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
//
//    }
//
//}