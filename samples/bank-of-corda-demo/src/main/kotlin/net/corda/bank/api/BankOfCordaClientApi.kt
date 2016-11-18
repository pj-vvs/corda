package net.corda.bank.api

import com.google.common.net.HostAndPort
import net.corda.bank.api.BankOfCordaWebApi.IssueRequestParams
import net.corda.bank.flow.IssuerFlow.IssuanceRequester
import net.corda.bank.flow.IssuerFlowResult
import net.corda.client.CordaRPCClient
import net.corda.core.contracts.Amount
import net.corda.core.contracts.currency
import net.corda.core.serialization.OpaqueBytes
import net.corda.node.services.config.configureTestSSL
import net.corda.node.services.messaging.startFlow
import net.corda.testing.http.HttpApi

/**
 * Interface for communicating with Bank of Corda node
 */
class BankOfCordaClientApi(val hostAndPort: HostAndPort) {
    /**
     * HTTP API
     */
    // TODO: security controls required
    fun requestWebIssue(params: IssueRequestParams): Boolean {
        val api = HttpApi.fromHostAndPort(hostAndPort, apiRoot)
        return api.postJson("issue-asset-request", params)
    }

    /**
     * RPC API
     */
    fun requestRPCIssue(params: IssueRequestParams): IssuerFlowResult {
        val client = CordaRPCClient(hostAndPort, configureTestSSL())
        // TODO: privileged security controls required
        client.start("user1","test")
        val proxy = client.proxy()

        val amount = Amount(params.amount, currency(params.currency))
        val issuerToPartyRef = OpaqueBytes.of(params.issueToPartyRefAsString.toByte())
        return proxy.startFlow(::IssuanceRequester, amount, params.issueToPartyName, issuerToPartyRef, BOC_ISSUER_PARTY.name).returnValue.toBlocking().first()
    }

    private companion object {
        private val apiRoot = "api/bank"
    }
}
