//package com.template.flows.UserFlow
//
//import co.paralleluniverse.fibers.Suspendable
//import com.template.contracts.UserContract
//import com.template.states.*
//import net.corda.core.contracts.*
//import net.corda.core.flows.*
//import net.corda.core.node.services.queryBy
//import net.corda.core.node.services.vault.QueryCriteria
//import net.corda.core.transactions.*
//import net.corda.core.utilities.ProgressTracker
//import net.corda.core.utilities.unwrap
//
//@InitiatingFlow
//@StartableByRPC
//class UpdateandRegisterFlow (private var name: UserDetails,
//                             private val counterparty: String,
//                             private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {
//    override val progressTracker: ProgressTracker = tracker()
//
//    companion object
//    {
//        object CREATING : ProgressTracker.Step("Creating registration!")
//        object SIGNING : ProgressTracker.Step("Signing registration!")
//        object VERIFYING : ProgressTracker.Step("Verifying registration!")
//        object FINALISING : ProgressTracker.Step("Finalize registration!") {
//            override fun childProgressTracker() = FinalityFlow.tracker()
//        }
//
//        fun tracker() = ProgressTracker(CREATING, SIGNING, VERIFYING, FINALISING)
//    }
//    @Suspendable
//    override fun call(): SignedTransaction {
//        progressTracker.currentStep = CREATING
//        val update = update()
//        progressTracker.currentStep = VERIFYING
//        progressTracker.currentStep = SIGNING
//        val signedTransaction = verifyandsign(transaction =  update)
//        val counterRef = serviceHub.identityService.partiesFromName(counterparty, false).singleOrNull()
//                ?: throw IllegalArgumentException("No match found for Owner $counterparty.")
//        val session = initiateFlow(counterRef)
//        session.send(name)
//        val transactionsigned = collectsignatures(signedTransaction,session =  listOf(session))
//        progressTracker.currentStep = FINALISING
//        return recordUpdate(transaction = transactionsigned, session =  listOf(session))
//
//    }
//    private fun updatestate(): UserState {
//        val input = inputstateref().state.data
//        if (name.fullname == "") name.fullname = input.name.fullname
//        if (name.age == "") name.age = input.name.age
//        if (name.gender == "") name.gender = input.name.gender
//        if (name.address == "") name.address = input.name.address
//        val counterRef = serviceHub.identityService.partiesFromName(counterparty, false).singleOrNull()
//                ?: throw IllegalArgumentException("No match found for Owner $counterparty.")
//        return UserState(name, ourIdentity, counterRef, input.verify, linearId = linearId)
//    }
//
//    private fun inputstateref(): StateAndRef<UserState> {
//        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
//        return serviceHub.vaultService.queryBy<UserState>(criteria).states.first()
//    }
//    private fun update(): TransactionBuilder {
//        val contract = UserContract.ID_Contracts
//        val notary = inputstateref().state.notary
//        val updateCommand =
//                Command(UserContract.Commands.Update(),
//                        updatestate().participants.map { it.owningKey })
//
//        return TransactionBuilder(notary = notary).withItems(inputstateref(), StateAndContract(updatestate(), contract), updateCommand)
//    }
//    private fun verifyandsign(transaction: TransactionBuilder): SignedTransaction {
//        transaction.verify(serviceHub)
//        return serviceHub.signInitialTransaction(transaction)
//    }
//    @Suspendable
//    private fun collectsignatures(transaction: SignedTransaction, session: List<FlowSession>): SignedTransaction =
//            subFlow(CollectSignaturesFlow(transaction, session))
//
//    @Suspendable
//    private fun recordUpdate(transaction: SignedTransaction, session: List<FlowSession>): SignedTransaction =
//            subFlow(FinalityFlow(transaction, session))
//
//    @InitiatedBy(UpdateandRegisterFlow::class)
//    class UpdateandRegisterFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {
//
//        @Suspendable
//        override fun call(): SignedTransaction {
//            val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
//                override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                    val output = stx.tx.outputs.single().data
//                    "This must be an IOU transaction" using (output is UserState)
//                }
//            }
//            val load = flowSession.receive(UserDetails::class.java).unwrap{it}
//            val txWeJustSignedId = subFlow(signedTransactionFlow)
//            subFlow(UserRegisterFlow(load))
//            return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
//        }
//    }
//}