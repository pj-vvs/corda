package net.corda.bank

import net.corda.bank.api.BankOfCordaClientApi
import net.corda.bank.api.BankOfCordaWebApi.IssueRequestParams
import net.corda.core.node.services.ServiceInfo
import net.corda.node.driver.driver
import net.corda.node.services.transactions.SimpleNotaryService
import net.corda.testing.getHostAndPort
import org.junit.Test

class BankOfCordaHttpAPITest {
    @Test fun `test issuer flow via Http`() {
        driver(dsl = {
            val nodeBankOfCorda = startNode("BankOfCorda", setOf(ServiceInfo(SimpleNotaryService.type))).get()
            val nodeBankOfCordaApiAddr = nodeBankOfCorda.config.getHostAndPort("webAddress")
            val nodeBigCorporation = startNode("BigCorporation").get()
            val bigCorporationName = nodeBigCorporation.nodeInfo.legalIdentity.name
            assert(BankOfCordaClientApi(nodeBankOfCordaApiAddr).requestWebIssue(IssueRequestParams(1000, "USD", bigCorporationName, "1")))
        }, isDebug = true)
    }
}