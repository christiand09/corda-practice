package com.template.webserver

import com.template.flows.cashflows.CashRegisterFlow
import com.template.flows.cashflows.CashSelfIssueFlow
import com.template.models.CashModel
import com.template.models.CashSelfIssueModel
import com.template.states.WalletState
import com.template.webserver.utilities.FlowHandlerCompletion
import net.corda.core.messaging.FlowHandle
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.transactions.SignedTransaction
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("cash") // The paths for HTTP requests are relative to this base path.
class CashController(rpc: NodeRPCConnection, private val flowHandlerCompletion : FlowHandlerCompletion) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    /**
     * Return all CashRegisterStates
     */

    @GetMapping("states/all", produces = ["application/json"])
    private fun getCashRegisterStates(): ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val infoStateRef = proxy.vaultQueryBy<WalletState>().states
            val infoStates = infoStateRef.map { it.state.data }
            val list = infoStates.map {
                CashModel(
                        wallet = it.wallet,
                        amountIssued = it.amountIssued,
                        amountPaid = it.amountPaid,
                        status = it.status,
                        borrower = it.borrower.toString(),
                        lender = it.lender.toString(),
                        admin = it.lender.toString(),
                        linearId = it.linearId
                )
            }
            HttpStatus.CREATED to list
        }
        catch (e: Exception)
        {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED)
        {
            "message" to "Successful"
        }
        else
        {
            "message" to "Failed"
        }

        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }

    /**
     * REGISTER - CashRegisterFlow
     */
    @PostMapping(value = "states/register", produces = ["application/json"])
    private fun cashRegister(): FlowHandle<SignedTransaction>
    {
        val flowReturn = proxy.startFlowDynamic(
                CashRegisterFlow::class.java
        )
        flowHandlerCompletion.flowHandlerCompletion(flowReturn)
        return flowReturn
    }

    /**
     * SELFISSUE - CashSelfIssueFlow
     */
    @PostMapping(value = "states/selfissue", produces = ["application/json"])
    private fun selfIssueCash(@RequestBody selfIssue: CashSelfIssueModel): ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val issue = CashSelfIssueModel(
                    selfIssueAmount = selfIssue.selfIssueAmount,
                    linearId = selfIssue.linearId
            )
            val flowReturn = proxy.startFlowDynamic(
                    CashSelfIssueFlow::class.java,
                    issue.selfIssueAmount,
                    issue.linearId
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to selfIssue
        }
        catch (e: Exception)
        {
            HttpStatus.BAD_REQUEST to e
        }

        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED)
        {
            "mesasge" to "Successful"
        }
        else
        {
            "message" to "Failed"
        }

        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }


}