package com.template.webserver

import com.template.flows.TokenFlow.*
import com.template.models.*
import com.template.states.TokenState
import com.template.webserver.utilities.FlowHandlerCompletion
import net.corda.core.messaging.vaultQueryBy
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/token")
class TokenController (rpc: NodeRPCConnection,val flowhandler: FlowHandlerCompletion) {
    private val proxy = rpc.proxy
    @GetMapping("/get", produces = ["application/json"])
    private fun getUser(): ResponseEntity<Map<String, Any>> {
        val (status, result) = try {

            val userget = proxy.vaultQueryBy<TokenState>().states
            val getStates = userget.map { it.state.data }
            val list = getStates.map {
                TokenModel(details = it.details, lender = it.lender.toString(),
                        borrower = it.borrower.toString(), requeststatus = it.requeststatus,
                        walletbalance = it.walletbalance, amountborrowed = it.amountborrowed,
                        amountpaid = it.amountpaid,linearId = it.linearId)
            }
            HttpStatus.CREATED to list
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful in getting ContractState of type UserState"
        } else {
            "message" to "Failed to get ContractState of type UserState"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }
    @PostMapping("/register",produces = ["application/json"])
    private fun registeruser(@RequestBody details: addUser): ResponseEntity<Map<String,Any>>
    {
        val (status,result) = try {
            val add = addUser(user = details.user)
            val flowReturn = proxy.startFlowDynamic(
                    TokenRegisterFlow::class.java,
                    add.user
            )
            flowhandler.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to details
        }
        catch (e:Exception)
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
    @PutMapping("/selfissue",produces = ["application/json"])
    private fun self(@RequestBody issueself: tokenselfIssue): ResponseEntity<Map<String,Any>>
    {
        val (status, result) = try {
            val tokenself = tokenselfIssue(amount = issueself.amount,linearId = issueself.linearId)
            val flowReturn = proxy.startFlowDynamic(
                    TokenSelfIssueFlow::class.java,
                    tokenself.amount,
                    tokenself.linearId
            )
            flowhandler.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to issueself
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
    @PutMapping("/issue",produces = ["application/json"])
    private fun issuetoanother(anotherissue: tokenissuetoanother): ResponseEntity<Map<String,Any>>
    {
        val (status,result) = try {
            val tokentoanother = tokenissuetoanother(amount = anotherissue.amount,
                                                     lender = anotherissue.lender,
                                                     linearId = anotherissue.linearId)
            val flowReturn = proxy.startFlowDynamic(
                    TokenIssueFlow::class.java,
                    tokentoanother.amount,
                    tokentoanother.lender,
                    tokentoanother.linearId
            )
            flowhandler.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to anotherissue
        }
        catch (e:Exception)
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
    @PutMapping("/transfer",produces = ["application/json"])
    private fun transfertoken(transfercash: tokentransfer): ResponseEntity<Map<String,Any>>
    {
        val (status,result) = try {

                val tokenTransfer = tokentransfer(linearId = transfercash.linearId,borrower = transfercash.borrower)
                val flowReturn = proxy.startFlowDynamic(
                        TokenTransferCashFlow::class.java,
                        tokenTransfer.linearId,
                        tokenTransfer.borrower
                )
                flowhandler.flowHandlerCompletion(flowReturn)
                HttpStatus.CREATED to transfercash
        }
        catch (e:Exception)
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
    @PutMapping("/settle",produces = ["application/json"])
    private fun tokensettle(settledebt: tokensettlebalance): ResponseEntity<Map<String,Any>>
    {
        val (status, result) = try {
            val settletoken = tokensettlebalance(linearId = settledebt.linearId,
                    lender = settledebt.lender,
                    amount = settledebt.amount)
            val flowReturn = proxy.startFlowDynamic(
                    TokenSettleCashFlow::class.java,
                    settletoken.linearId,
                    settletoken.lender,
                    settletoken.amount
            )
            flowhandler.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to settledebt
        }
        catch (e:Exception)
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
}
