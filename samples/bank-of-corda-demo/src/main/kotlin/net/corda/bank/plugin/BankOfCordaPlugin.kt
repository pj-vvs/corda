package net.corda.bank.plugin

import com.esotericsoftware.kryo.Kryo
import net.corda.bank.api.BankOfCordaWebApi
import net.corda.bank.flow.IssuerFlow
import net.corda.bank.flow.IssuerFlowResult
import net.corda.core.contracts.Amount
import net.corda.core.node.CordaPluginRegistry
import net.corda.core.serialization.OpaqueBytes

class BankOfCordaPlugin : CordaPluginRegistry() {
    // A list of classes that expose web APIs.
    override val webApis: List<Class<*>> = listOf(BankOfCordaWebApi::class.java)
    // A list of flow that are required for this cordapp
    override val requiredFlows: Map<String, Set<String>> =
        mapOf(IssuerFlow.IssuanceRequester::class.java.name to setOf(Amount::class.java.name, String::class.java.name, OpaqueBytes::class.java.name, String::class.java.name)
    )
    override val servicePlugins: List<Class<*>> = listOf(IssuerFlow.Issuer.Service::class.java)

    override fun registerRPCKryoTypes(kryo: Kryo): Boolean {
        kryo.register(IssuerFlowResult.Success::class.java)
        kryo.register(IssuerFlowResult.Failed::class.java)
        return true
    }
}
