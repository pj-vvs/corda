package net.corda.loadtest.tests

import de.danielbechler.diff.ObjectDifferFactory
import net.corda.client.mock.Generator
import net.corda.client.mock.pickOne
import net.corda.client.mock.replicatePoisson
import net.corda.contracts.asset.Cash
import net.corda.core.contracts.USD
import net.corda.core.crypto.Party
import net.corda.flows.CashCommand
import net.corda.flows.CashFlow
import net.corda.flows.CashFlowResult
import net.corda.loadtest.LoadTest
import net.corda.loadtest.NodeHandle
import net.corda.node.services.messaging.startFlow
import org.slf4j.LoggerFactory
import java.util.*

private val log = LoggerFactory.getLogger("SelfIssue")

// DOCS START 1
data class SelfIssueCommand(
        val command: CashCommand.IssueCash,
        val node: NodeHandle
)

data class SelfIssueState(
        val vaultsSelfIssued: Map<Party, Long>
) {
    fun copyVaults(): HashMap<Party, Long> {
        return HashMap(vaultsSelfIssued)
    }
}

val selfIssueTest = LoadTest<SelfIssueCommand, SelfIssueState>(
        // DOCS END 1
        "Self issuing cash randomly",

        generate = { state, parallelism ->
            val generateIssue = Generator.pickOne(simpleNodes).bind { node: NodeHandle ->
                generateIssue(1000, USD, notary.info.notaryIdentity, listOf(node.info.legalIdentity)).map {
                    SelfIssueCommand(it, node)
                }
            }
            Generator.replicatePoisson(parallelism.toDouble(), generateIssue).bind {
                // We need to generate at least one
                if (it.isEmpty()) {
                    Generator.sequence(listOf(generateIssue))
                } else {
                    Generator.pure(it)
                }
            }
        },

        interpret = { state, command ->
            val vaults = state.copyVaults()
            val issuer = command.node.info.legalIdentity
            vaults.put(issuer, (vaults[issuer] ?: 0L) + command.command.amount.quantity)
            SelfIssueState(vaults)
        },

        execute = { command ->
            val result = command.node.connection.proxy.startFlow(::CashFlow, command.command).returnValue.toBlocking().first()
            when (result) {
                is CashFlowResult.Success -> {
                    log.info(result.message)
                }
                is CashFlowResult.Failed -> {
                    log.error(result.message)
                }
            }
        },

        gatherRemoteState = { previousState ->
            val selfIssueVaults = HashMap<Party, Long>()
            simpleNodes.forEach { node ->
                val vault = node.connection.proxy.vaultAndUpdates().first
                vault.forEach {
                    val state = it.state.data
                    if (state is Cash.State) {
                        val issuer = state.amount.token.issuer.party
                        if (issuer == node.info.legalIdentity) {
                            selfIssueVaults.put(issuer, (selfIssueVaults[issuer] ?: 0L) + state.amount.quantity)
                        }
                    }
                }
            }
            log.info("$selfIssueVaults")
            if (previousState != null) {
                val diff = ObjectDifferFactory.getInstance().compare(previousState.vaultsSelfIssued, selfIssueVaults)
                if (!diff.isUntouched) {

                    var diffString = ""
                    diff.visit { node, visit ->
                        if (node.isChanged && node.children.all { !it.isChanged }) {
                            diffString += "${node.propertyPath}: simulated[${node.canonicalGet(previousState.vaultsSelfIssued)}], actual[${node.canonicalGet(selfIssueVaults)}]\n"
                        }
                    }
                    throw Exception(
                            "Simulated state diverged from actual state" +
                                    "\nSimulated state:\n${previousState.vaultsSelfIssued}" +
                                    "\nActual state:\n$selfIssueVaults" +
                                    "\nDiff:\n$diffString"
                    )
                }
            }
            SelfIssueState(selfIssueVaults)
        }
)
