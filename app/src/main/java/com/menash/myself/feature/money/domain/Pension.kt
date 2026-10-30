package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

enum class FundKind { PENSION, HISHTALMUT, GEMEL }

data class PensionFund(
    val id: String, val kind: FundKind, val provider: String,
    val value: Money, val totalDeposits: Money, val monthlyDeposit: Money,
    val feeOnBalance: BigDecimal, val feeOnDeposits: BigDecimal,
    val openedOn: LocalDate?, val lastUpdated: LocalDate,
) {
    init {
        require(id.isNotBlank())
        require(listOf(value, totalDeposits, monthlyDeposit).all { it >= Money.ZERO })
        require(listOf(feeOnBalance, feeOnDeposits).all { it >= BigDecimal.ZERO && it <= BigDecimal.TEN })
    }
}

data class FundSnapshot(val profit: Money, val yearlyFee: Money, val liquidOn: LocalDate?)
data class PensionSnapshot(
    val value: Money, val deposits: Money, val profit: Money, val yearlyFee: Money,
    val priciest: PensionFund?, val liquidNow: List<PensionFund>, val stale: List<PensionFund>,
)

private fun PensionFund.exactFee(): BigDecimal =
    value.agorot.toBigDecimal().multiply(feeOnBalance)
        .add(monthlyDeposit.agorot.toBigDecimal().multiply(BigDecimal(12)).multiply(feeOnDeposits))
        .movePointLeft(2)
private fun BigDecimal.pensionMoney() = Money(setScale(0, RoundingMode.HALF_UP).longValueExact())
private fun PensionFund.liquidOn(): LocalDate? =
    if (kind == FundKind.HISHTALMUT) openedOn?.plusYears(6) else null

fun fundSnapshot(fund: PensionFund): FundSnapshot = FundSnapshot(
    Money(Math.subtractExact(fund.value.agorot, fund.totalDeposits.agorot)),
    fund.exactFee().pensionMoney(), fund.liquidOn(),
)

/** Current values only; yearly fees are an estimate at today's balance and monthly deposit. */
fun pensionSnapshot(funds: List<PensionFund>, today: LocalDate): PensionSnapshot {
    require(funds.map { it.id }.distinct().size == funds.size) { "Fund ids must be unique" }
    val value = sumMoney(funds.map { it.value })
    val deposits = sumMoney(funds.map { it.totalDeposits })
    val fees = funds.associateWith { it.exactFee() }
    return PensionSnapshot(
        value, deposits, Money(Math.subtractExact(value.agorot, deposits.agorot)),
        fees.values.fold(BigDecimal.ZERO, BigDecimal::add).pensionMoney(),
        funds.filter { fees.getValue(it).pensionMoney() > Money.ZERO }
            .sortedWith(compareByDescending<PensionFund> { fees.getValue(it).pensionMoney() }.thenBy { it.id }).firstOrNull(),
        funds.filter { it.liquidOn()?.let { date -> date <= today } == true },
        funds.filter { today > it.lastUpdated.plusMonths(4) },
    )
}
