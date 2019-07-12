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
import net.corda.core.contracts.Command



abstract class FlowFunction : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker(INITIALIZING, BUILDING, SIGNING, COLLECTING, FINALIZING)

    fun inputStateRef(linearId: UniqueIdentifier): StateAndRef<MyState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<MyState>(criteria).states.single()
    }

    fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        progressTracker.currentStep = SIGNING
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }


    @Suspendable
    fun collectSignature(
            transaction: SignedTransaction,
            sessions: List<FlowSession>
    ): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, sessions))

    @Suspendable
    fun recordTransactionWithOtherParty(transaction: SignedTransaction, sessions: List<FlowSession>) : SignedTransaction {
        progressTracker.currentStep = FINALIZING
        return subFlow(FinalityFlow(transaction, sessions))
    }

    /** OR  */

    @Suspendable
    private fun recordTransaction(transaction: SignedTransaction, sessions: List<FlowSession>): SignedTransaction =
            subFlow(FinalityFlow(transaction, sessions))

    @Suspendable
    fun recordTransactionWithoutOtherParty(transaction: SignedTransaction) : SignedTransaction {
        progressTracker.currentStep = FINALIZING
        return subFlow(FinalityFlow(transaction, emptyList()))
    }

    fun stringToParty(name: String): Party {
        return serviceHub.identityService.partiesFromName(name, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for $name")
    }

    fun stringToUniqueIdentifier(id: String): UniqueIdentifier {
        return UniqueIdentifier.fromString(id)
    }

    fun inputStateAndRefUserState(id: String): StateAndRef<MyState> {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(stringToUniqueIdentifier(id)))
        return serviceHub.vaultService.queryBy<MyState>(queryCriteria).states.single()
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