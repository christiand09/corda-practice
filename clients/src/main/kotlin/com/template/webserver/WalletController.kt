package com.template.webserver

import com.template.flows.cashflows.*
import com.template.models.*
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
@RequestMapping("wallet") // The paths for HTTP requests are relative to this base path.
class WalletController(rpc: NodeRPCConnection, private val flowHandlerCompletion : FlowHandlerCompletion) {

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
                WalletModel(
                        wallet = it.wallet,
                        amountIssued = it.amountIssued,
                        amountPaid = it.amountPaid,
                        status = it.status,
                        borrower = it.borrower.toString(),
                        lender = it.lender.toString(),
                        admin = it.admin.toString(),
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
    private fun selfIssueCash(@RequestBody selfIssue: WalletSelfIssueModel): ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val issue = WalletSelfIssueModel(
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

    /**
     * ISSUE - CashIssueFlow
     */
    @PostMapping(value = "states/issue", produces = ["application/json"])
    private fun issueCash(@RequestBody issue: WalletIssueModel): ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val issueModel = WalletIssueModel(
                    amountIssue = issue.amountIssue,
                    counterParty = issue.counterParty,
                    linearId = issue.linearId
            )
            val flowReturn = proxy.startFlowDynamic(
                    CashIssueFlow::class.java,
                    issueModel.amountIssue,
                    issueModel.counterParty,
                    issueModel.linearId
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to issue
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

    /**
     * TRANSFER - CashTransferFlow
     */
    @PostMapping(value = "states/transfer", produces = ["application/json"])
    private fun transferCash(@RequestBody transfer: WalletTransferModel): ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val transferModel = WalletTransferModel(
                    amountTransfer = transfer.amountTransfer,
                    counterParty = transfer.counterParty,
                    linearId = transfer.linearId
            )
            val flowReturn = proxy.startFlowDynamic(
                    CashTransferFlow::class.java,
                    transferModel.amountTransfer,
                    transferModel.counterParty,
                    transferModel.linearId
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to transfer
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

    /**
     * SETTLE - CashSettleFlow
     */
    @PostMapping(value = "states/settle", produces = ["application/json"])
    private fun settleCash(@RequestBody settle: WalletSettleModel): ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val settleModel = WalletSettleModel(
                    amountSettle = settle.amountSettle,
                    counterParty = settle.counterParty,
                    linearId = settle.linearId
            )
            val flowReturn = proxy.startFlowDynamic(
                    CashSettleFlow::class.java,
                    settleModel.amountSettle,
                    settleModel.counterParty,
                    settleModel.linearId
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to settle
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