package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.MyContract
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
//import org.apache.commons.mail.DefaultAuthenticator
//import org.apache.commons.mail.HtmlEmail
import com.template.states.MyState
import com.template.states.TimeWindowState
import net.corda.core.contracts.Command
import java.time.Instant


abstract class FlowFunction : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker(INITIALIZING, BUILDING, SIGNING, COLLECTING, FINALIZING)

    fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        progressTracker.currentStep = SIGNING
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    @Suspendable
    fun collectSignature(transaction: SignedTransaction, sessions: List<FlowSession>): SignedTransaction {
        progressTracker.currentStep = COLLECTING
        return subFlow(CollectSignaturesFlow(transaction, sessions))}

    @Suspendable
    fun recordTransaction(transaction: SignedTransaction, sessions: List<FlowSession>): SignedTransaction {
        progressTracker.currentStep = FINALIZING
        return subFlow(FinalityFlow(transaction, sessions))
    }


    fun stringToParty(name: String): Party {
        return serviceHub.identityService.partiesFromName(name, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for $name")
    }

    fun stringToPartySpy(name: String): Party {
        return serviceHub.identityService.partiesFromName(name, false).first()
                ?: throw IllegalArgumentException("No match found for $name")
    }

    fun inputStateRef(linearId: UniqueIdentifier): StateAndRef<MyState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<MyState>(criteria).states.single()
    }

    fun iState(linearId: UniqueIdentifier): StateAndRef<TimeWindowState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<TimeWindowState>(criteria).states.single()
    }

    fun stringToUniqueIdentifier(id: String): UniqueIdentifier {
        return UniqueIdentifier.fromString(id)
    }

    fun inputStateAndRef(id: String): StateAndRef<MyState> {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(stringToUniqueIdentifier(id)))
        return serviceHub.vaultService.queryBy<MyState>(queryCriteria).states.single()
    }

    fun getTime(id: UniqueIdentifier): Instant {
        val outputStateRef = net.corda.core.contracts.StateRef(txhash = iState(id).ref.txhash, index = 0)
        val queryCriteria = QueryCriteria.VaultQueryCriteria(stateRefs = listOf(outputStateRef))
        val results = serviceHub.vaultService.queryBy<TimeWindowState>(queryCriteria)
        return results.statesMetadata.single().recordedTime.plusSeconds(30)
    }


    /** Might need **/

//    fun sendEmailApproved(emailUser: String, name: String, currentUserId: String, paramUserId: String){
//        if (currentUserId == paramUserId){
//            val senderEmail = "promethium.email@gmail.com"
//            val password = "thisisthepassword"
//            val toMail = emailUser
//
//            val email = HtmlEmail()
//            email.hostName = "smtp.googlemail.com"
//            email.setSmtpPort(465)
//            email.setAuthenticator(DefaultAuthenticator(senderEmail, password))
//            email.isSSLOnConnect = true
//            email.setFrom(senderEmail)
//            email.addTo(toMail)
//            email.subject = "Approved user"
//            email.setHtmlMsg("<html><h4>Hi  "+ "ISS" +",</h4>" +
//                    "<br>"+ name + "is approved"+"<br>" +
//                    "<h4>Regards,<br>Insurance Small Small</h4></html>")
//            email.send()
//        }
//    }
//
//    fun sendEmailRegister(emailUser: String, name: String, userType: String) {
//        if (userType == "User" || userType == "Insurance" || userType == "Broker" || userType == "Bank" ) {
//            val senderEmail = "promethium.email@gmail.com"
//            val password = "thisisthepassword"
//            val toMail = emailUser
//
//            val email = HtmlEmail()
//            email.hostName = "smtp.googlemail.com"
//            email.setSmtpPort(465)
//            email.setAuthenticator(DefaultAuthenticator(senderEmail, password))
//            email.isSSLOnConnect = true
//            email.setFrom(senderEmail)
//            email.addTo(toMail)
//            email.subject = "Welcome to ISS"
//            email.setHtmlMsg("<html><h4>Hi  " + name+ ",</h4>" +
//                    "<br>" + "Please click the link below"+"<br>" +
//                    "<br><a>" + "http://iss.quantumcrowd.io/user/kyc"+"</a><br>" +
//                    "<h4>Regards,<br>Insurance Small Small</h4></html>")
//            email.send()
//        }
//    }





}