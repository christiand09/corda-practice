package com.template.webserver


import com.template.flows.TokenFlows.PartyIssueFlow
import com.template.flows.TokenFlows.PartyIssueTransferFlow
import com.template.flows.TokenFlows.PartySettleFlow
import com.template.flows.TokenFlows.UserWithTokenRegisterFlow
import com.template.models.*
import com.template.states.TokenState
import com.template.webserver.utilities.FlowHandlerCompletion
import net.corda.core.messaging.FlowHandle
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.transactions.SignedTransaction
import org.springframework.web.bind.annotation.*


/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/token") // The paths for HTTP requests are relative to this base path.
class TokenController(rpc: NodeRPCConnection, val flowHandlerCompletion: FlowHandlerCompletion) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

//  @GetMapping(value = "/templateendpoint", produces = arrayOf("text/plain"))
//    private fun templateendpoint(): String {
//        return "Define an endpoint here."
//    }


    @GetMapping(value = "/token/state", produces = arrayOf("application/json"))
    private fun getTokenState(): ResponseEntity<Map<String, Any>> {
        val (status, result) = try {
            val TokenStateRef = proxy.vaultQueryBy<TokenState>().states
            val tokenState = TokenStateRef.map { it.state.data }
            val list = tokenState.map {

                TokenModel(amountIssued = it.amountIssued,
                        amountPaid = it.amountPaid,
                        borrower = it.borrower.toString(),
                        lender = it.lender.toString(),
                        iss = it.iss.toString(),
                        walletBalance = it.walletBalance,
                        settled = it.settled,
                        linearId = it.linearId)
            }
            HttpStatus.CREATED to list
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status

        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful"
        } else {
            "message" to "Failed"
        }
        val res = "result" to result

        return ResponseEntity.status(status).body(mapOf(stat, mess, res))

    }

    @PostMapping(value = "states/register", produces = ["application/json"])

    private fun TokenRegister(): FlowHandle<SignedTransaction> {

        val flowReturn = proxy.startFlowDynamic(

                UserWithTokenRegisterFlow::class.java
        )
        flowHandlerCompletion.flowHandlerCompletion(flowReturn)

        return flowReturn

    }

    @PostMapping(value = "/token/issue", produces = arrayOf("application/json"))

    private fun TokenIssueModel(@RequestBody TokenIssue: TokenIssueModel): ResponseEntity<Map<String, Any>> {
        val (status, result) = try {
            val TokenIssued = TokenIssueModel(amountIssued = TokenIssue.amountIssued,
                                              counterParty = TokenIssue.counterParty,
                                              linearId = TokenIssue.linearId)

            val flowReturn = proxy.startFlowDynamic(

                    PartyIssueFlow::class.java,
                    TokenIssued.amountIssued,
                    TokenIssued.counterParty,
                    TokenIssued.linearId
            )

            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to TokenIssue

        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "$e"
        }
        val stat = "status" to status.value()

        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful"
        } else {
            "message" to "Failed"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }

    @PutMapping(value = "/token/transfer", produces = arrayOf("application/json"))

    private fun TokenTransferModel(@RequestBody TokenTransfer: TokenTransferModel): ResponseEntity<Map<String, Any>> {
        val (status, result) = try {
            val TokenTransfered = TokenTransferModel(amountToLend = TokenTransfer.amountToLend,
                                                     counterParty = TokenTransfer.counterParty,
                                                     linearId = TokenTransfer.linearId)

            val flowReturn = proxy.startFlowDynamic(

                    PartyIssueTransferFlow::class.java,
                    TokenTransfered.amountToLend,
                    TokenTransfered.counterParty,
                    TokenTransfered.linearId
            )

            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to TokenTransfer

        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "$e"
        }
        val stat = "status" to status.value()

        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful"
        } else {
            "message" to "Failed"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }
//
//
    @PutMapping(value = "/token/settle", produces = arrayOf("application/json"))

    private fun TokenSettleModel(@RequestBody TokenSettle: TokenSettleModel) : ResponseEntity<Map<String,Any>> {
        val (status, result) = try {
            val TokenSettled = TokenSettleModel(amountToPay = TokenSettle.amountToPay ,
                                                counterParty = TokenSettle.counterParty ,
                                                linearId = TokenSettle.linearId)

            val flowReturn = proxy.startFlowDynamic(

                    PartySettleFlow::class.java,
                    TokenSettled.amountToPay,
                    TokenSettled.counterParty,
                    TokenSettled.linearId
            )

            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to TokenSettle

        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "$e"
        }
        val stat = "status" to status.value()

        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful"
        } else {
            "message" to "Failed"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }

    @PutMapping(value = "/token/selfIssue", produces = arrayOf("application/json"))

    private fun TokenSelfIssueModel(@RequestBody TokenSelfIssue: TokenSelfIssueModel) : ResponseEntity<Map<String,Any>> {
        val (status, result) = try {
            val TokenSelfIssued = TokenSelfIssueModel(walletBalance = TokenSelfIssue.walletBalance,linearId =TokenSelfIssue.linearId )

            val flowReturn = proxy.startFlowDynamic(

                    PartySettleFlow::class.java,
                    TokenSelfIssued.walletBalance,
                    TokenSelfIssued.linearId

            )

            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to TokenSelfIssue

        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "$e"
        }
        val stat = "status" to status.value()

        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful"
        } else {
            "message" to "Failed"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }

}