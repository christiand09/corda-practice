package com.template.contracts

import com.template.states.WalletState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

 class WalletContract: Contract {
    companion object {
        @JvmStatic
        // Used to identify our contract when building a transaction.
        val WALLET_CONTRACT_ID = WalletContract::class.qualifiedName!!
    }

     interface Commands : CommandData {
         class WalletRegister: TypeOnlyCommandData(), Commands
         class RequestIssue : TypeOnlyCommandData(), Commands
         class SelfIssue : TypeOnlyCommandData(), Commands
         class Transfer : TypeOnlyCommandData(), Commands
         class Settle : TypeOnlyCommandData(), Commands
     }

     override fun verify(tx: LedgerTransaction) {
         val command = tx.commands.requireSingleCommand<WalletContract.Commands>()
         when(command.value) {
             is Commands.WalletRegister -> requireThat {

             }
             is Commands.RequestIssue -> requireThat {

             }
             is Commands.SelfIssue -> requireThat {

             }
             is Commands.Transfer -> requireThat {

             }
             is Commands.Settle -> requireThat {


             }

         }
     }
}
