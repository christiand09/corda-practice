package com.template.webserver

import com.template.flows.thirdPartySpy.*
import com.template.model.*
import com.template.states.KYCSpyState
import com.template.webserver.utilities.FlowHandlerCompletion
import net.corda.core.messaging.FlowHandle
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.transactions.SignedTransaction
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private const val CONTROLLER_NAME = "config.controller.name"
//@Value("\${$CONTROLLER_NAME}") private val controllerName: String
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class KYCController(
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
    @GetMapping(value = ["/kyc/user"], produces = ["application/json"])
    private fun KYCUserModels() : ResponseEntity<Map<String, Any>> {
        val (status, result ) = try {
            val requestStateRef = rpc.proxy.vaultQueryBy<KYCSpyState>().states
            val requestStates = requestStateRef.map { it.state.data }
            val list = requestStates.map {
                KYCUserModel(
                        moneyLend = it.moneyLend,
                        moneyBalance = it.moneyBalance,
                        requestedAmount = it.requestedAmount,
                        lender = it.lender.toString(),
                        borrower = it.borrower.toString(),
                        spy = it.spy.toString(),
                        status = it.status,
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

    @PostMapping(value = ["/kyc/user/register"], produces = ["application/json"])
    private fun KYCSpyRegister(): FlowHandle<SignedTransaction> {
        val flowReturn = proxy.startFlowDynamic(

                KYCSpyRegisterFlow()::class.java
        )
        flowHandlerCompletion.flowHandlerCompletion(flowReturn)

        return flowReturn
    }

    /**
     * SELFISSUE - UserAccountVerifyFlow
     */

    @PostMapping(value = ["/kyc/user/selfIssue"], produces = ["application/json"])
    private fun kycIssueModel(@RequestBody kycIssueModel: KYCIssueModel) : ResponseEntity<Map<String,Any>> {

        val (status, result) = try {
            val user = KYCIssueModel(
                    amount = kycIssueModel.amount,
                    linearId = kycIssueModel.linearId

            )
            val flowReturn = proxy.startFlowDynamic(
                    KYCSpySelfIssueFlow::class.java,
                    user.amount,
                    user.linearId
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to kycIssueModel
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
     * KYCRequestModel - UserAccountVerifyFlow
     */

    @PostMapping(value = ["/kyc/user/request"], produces = ["application/json"])
    private fun kycRequestModel(@RequestBody kycRequestModel: KYCRequestModel) : ResponseEntity<Map<String,Any>> {

        val (status, result) = try {
            val user = KYCRequestModel(
                    amount = kycRequestModel.amount,
                    lender = kycRequestModel.lender,
                    linearId = kycRequestModel.linearId

            )
            val flowReturn = proxy.startFlowDynamic(
                    KYCSpyIssueTransactionFlow::class.java,
                    user.amount,
                    user.lender,
                    user.linearId
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to kycRequestModel
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
     * KYCTransferModel - UserAccountVerifyFlow
     */

    @PostMapping(value = ["/kyc/user/transfer"], produces = ["application/json"])
    private fun kycTransferModel(@RequestBody kycTransferModel: KYCTransferModel) : ResponseEntity<Map<String,Any>> {

        val (status, result) = try {
            val user = KYCTransferModel(

                    borrower = kycTransferModel.borrower,
                    linearId = kycTransferModel.linearId

            )
            val flowReturn = proxy.startFlowDynamic(
                    KYCSpyTransferFlow::class.java,

                    user.borrower,
                    user.linearId
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to kycTransferModel
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
     * KYCSettleModel - UserAccountVerifyFlow
     */

    @PostMapping(value = ["/kyc/user/settle"], produces = ["application/json"])
    private fun kycSettleModel(@RequestBody kycSettleModel: KYCSettleModel) : ResponseEntity<Map<String,Any>> {

        val (status, result) = try {
            val user = KYCSettleModel(
                    amountPay = kycSettleModel.amountPay,
                    lender = kycSettleModel.lender,
                    linearId = kycSettleModel.linearId

            )
            val flowReturn = proxy.startFlowDynamic(
                    KYCSpySettleFlow::class.java,
                    user.amountPay,
                    user.lender,
                    user.linearId
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to kycSettleModel
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

}