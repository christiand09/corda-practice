package com.template.webserver

import com.template.flows.walletFlows.*
import com.template.models.*
import com.template.states.WalletState
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping ("/")
class WalletController (private val rpc: NodeRPCConnection) {
    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }
    private val proxy = rpc.proxy

    @GetMapping ("wallet/state", produces = arrayOf("application/json"))
    private fun getWalletState(): ResponseEntity<Map<String,Any>> {
        val (status, result) = try {
            val walletStateRef = rpc.proxy.vaultQueryBy<WalletState>().states
            val requestState = walletStateRef.map { it.state.data }
            val list = requestState.map {
                CashModel (
                        name = it.name,
                        walletBalance = it.walletBalance,
                        amountBorrowed = it.amountBorrowed,
                        issueStatus = it.issueStatus,
                        amountPaid = it.amountPaid,
                        lender = it.lender.toString(),
                        borrower = it.borrower.toString(),
                        admin = it.admin.toString(),
                        linearId = it.linearId
                )
            }
            HttpStatus.CREATED to list
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
     *   WALLET REGISTER - Wallet Register Flow
     */
    @PostMapping("wallet/register", produces = arrayOf("application/json"))
    private fun walletRegister(@RequestBody walletRegister: WalletRegisterAccount) : ResponseEntity<Map<String,Any>> {
        val (status, result) = try {
            val register = WalletRegisterAccount(
                    name = walletRegister.name
            )
            proxy.startFlowDynamic(WalletRegisterFlow::class.java,
                    register.name)

            HttpStatus.CREATED to walletRegister
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
     *   WALLET SELF ISSUE - Self Issue Flow
     */
    @PutMapping("wallet/selfissue", produces = arrayOf("application/json"))
    private fun walletSelfIssue(@RequestBody walletSelfIssue: WalletSelfIssueAccount) : ResponseEntity<Map<String,Any>> {
        val (status, result) = try {
            val selfissue = WalletSelfIssueAccount (
                    amount = walletSelfIssue.amount,
                    linearId = walletSelfIssue.linearId)

            proxy.startFlowDynamic(SelfIssueFlow::class.java,
                    selfissue.amount,
                    selfissue.linearId)

            HttpStatus.CREATED to walletSelfIssue
        } catch (e: java.lang.Exception) {
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
     *   WALLET ISSUE - Issue Flow
     */
    @PostMapping("wallet/issue", produces = arrayOf("application/json"))
    private fun walletIssue(@RequestBody walletIssue: WalletIssueAccount) : ResponseEntity<Map<String,Any>> {

        val (status, result) = try {
            val issue = WalletIssueAccount(
                    amount = walletIssue.amount,
                    counterParty = walletIssue.counterParty,
                    linearId = walletIssue.linearId)

            proxy.startFlowDynamic(IssueFlow::class.java,
                    issue.amount,
                    issue.counterParty,
                    issue.linearId)

            HttpStatus.CREATED to walletIssue
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
     *   WALLET TRANSFER - Transfer Flow
     */
    @PutMapping("wallet/transfer", produces = arrayOf("application/json"))
    private fun walletTransfer(@RequestBody walletTransfer: WalletTransferAccount) : ResponseEntity<Map<String,Any>> {

        val (status, result) = try {
            val transfer = WalletTransferAccount(
                    amount = walletTransfer.amount,
                    counterParty = walletTransfer.counterParty,
                    linearId = walletTransfer.linearId)

            proxy.startFlowDynamic(TransferFlow::class.java,
                    transfer.amount,
                    transfer.counterParty,
                    transfer.linearId)

            HttpStatus.CREATED to walletTransfer
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
     *   WALLET SETTLE - Settle Flow
     */
    @PutMapping("wallet/settle", produces = arrayOf("application/json"))
    private fun walletSettle(@RequestBody walletSettle: WalletTransferAccount) : ResponseEntity<Map<String,Any>> {

        val (status, result) = try {
            val settle = WalletSettleAccount(
                    amount = walletSettle.amount,
                    counterParty = walletSettle.counterParty,
                    linearId = walletSettle.linearId)

            proxy.startFlowDynamic(SettleFlow::class.java,
                    settle.amount,
                    settle.counterParty,
                    settle.linearId)

            HttpStatus.CREATED to walletSettle
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

