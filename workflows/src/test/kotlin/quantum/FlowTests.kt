package quantum

import com.template.contracts.WalletContract
import com.template.flows.cashflows.CashRegisterFlow
import com.template.flows.cashflows.CashRegisterFlowResponder
import com.template.flows.testflows.TestRegisterFlow
import com.template.flows.testflows.TestRegisterFlowResponder
import com.template.states.TestState
import com.template.states.WalletState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class FlowTests
{
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows.testflows")
    )))

    private val a = network.createNode()

    init
    {
        listOf(a).forEach {
            it.registerInitiatedFlow(TestRegisterFlowResponder::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `borrower and lender must be the same`()
    {
        val register = TestRegisterFlow()
        val future = a.startFlow(register)
        setup()
        val ptx = future.getOrThrow()
        assert(ptx.tx.inputs.isEmpty())
        assert(ptx.tx.outputs.single().data is TestState)
    }
}