package com.menash.myself.feature.money.data

import com.menash.myself.feature.money.domain.FundKind
import com.menash.myself.feature.money.domain.PensionFund
import java.math.BigDecimal
import java.time.LocalDate

/** Pension, hishtalmut and gemel funds, in the same conventions as [MoneyDocuments]. Fees are decimal strings. */
internal object PensionDocuments {

    fun encode(fund: PensionFund): Map<String, Any> = buildMap {
        put("kind", fund.kind.name)
        put("provider", fund.provider)
        put("value", fund.value.agorot)
        put("totalDeposits", fund.totalDeposits.agorot)
        put("monthlyDeposit", fund.monthlyDeposit.agorot)
        put("feeOnBalance", fund.feeOnBalance.toPlainString())
        put("feeOnDeposits", fund.feeOnDeposits.toPlainString())
        fund.openedOn?.let { put("openedOn", it.toString()) }
        put("lastUpdated", fund.lastUpdated.toString())
    }

    fun fund(id: String, doc: MoneyDoc): PensionFund = PensionFund(
        id = id,
        kind = doc.enumOrNull("kind", FundKind.entries) ?: FundKind.PENSION,
        provider = doc.string("provider"),
        value = doc.money("value"),
        totalDeposits = doc.money("totalDeposits"),
        monthlyDeposit = doc.money("monthlyDeposit"),
        feeOnBalance = BigDecimal(doc.string("feeOnBalance")),
        feeOnDeposits = BigDecimal(doc.string("feeOnDeposits")),
        openedOn = doc.stringOrNull("openedOn")?.let(LocalDate::parse),
        lastUpdated = LocalDate.parse(doc.string("lastUpdated")),
    )
}
