package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.FundKind
import com.menash.myself.feature.money.domain.FundSnapshot
import com.menash.myself.feature.money.domain.PensionFund
import com.menash.myself.feature.money.domain.PensionSnapshot
import com.menash.myself.feature.money.domain.fundSnapshot
import com.menash.myself.feature.money.domain.pensionSnapshot
import java.math.BigDecimal
import java.time.LocalDate

/** One fund as the screen shows it; every number comes from the engine's fundSnapshot / pensionSnapshot. */
data class FundRow(val fund: PensionFund, val snapshot: FundSnapshot, val stale: Boolean, val priciest: Boolean, val liquid: Boolean)

/** "פנסיה וחיסכון": today's picture only. [total] is null while no fund is stored. */
data class PensionView(val rows: List<FundRow>, val total: PensionSnapshot?)

private val kindOrder = listOf(FundKind.PENSION, FundKind.HISHTALMUT, FundKind.GEMEL)

fun pensionView(funds: List<PensionFund>, today: LocalDate): PensionView {
    if (funds.isEmpty()) return PensionView(emptyList(), null)
    val total = pensionSnapshot(funds, today)
    val stale = total.stale.map { it.id }.toSet()
    val liquid = total.liquidNow.map { it.id }.toSet()
    val rows = funds.sortedWith(compareBy<PensionFund> { kindOrder.indexOf(it.kind) }.thenBy { it.provider }).map {
        FundRow(it, fundSnapshot(it), it.id in stale, it.id == total.priciest?.id, it.id in liquid)
    }
    return PensionView(rows, total)
}

/** The fund sheet; numbers stay text until save. Fees are percents, e.g. "0.22". */
data class FundDraft(
    val id: String? = null,
    val kind: FundKind = FundKind.PENSION,
    val provider: String = "",
    val value: String = "",
    val totalDeposits: String = "",
    val monthlyDeposit: String = "",
    val feeOnBalance: String = "",
    val feeOnDeposits: String = "",
    val openedOn: LocalDate? = null,
)

enum class FundField { PROVIDER, VALUE, DEPOSITS, MONTHLY, FEE_BALANCE, FEE_DEPOSITS }

fun fundDraftOf(fund: PensionFund) = FundDraft(
    fund.id, fund.kind, fund.provider, amountText(fund.value), amountText(fund.totalDeposits), amountText(fund.monthlyDeposit),
    fund.feeOnBalance.stripTrailingZeros().toPlainString(), fund.feeOnDeposits.stripTrailingZeros().toPlainString(), fund.openedOn,
)

sealed interface FundResult {
    data class Ok(val fund: PensionFund) : FundResult
    data class Invalid(val field: FundField) : FundResult
}

private fun fee(text: String): BigDecimal? {
    val trimmed = text.trim().removeSuffix("%").trim()
    if (trimmed.isEmpty()) return BigDecimal.ZERO
    return trimmed.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO && it <= BigDecimal.TEN }
}

private fun amount(text: String, required: Boolean): Money? =
    if (text.isBlank() && !required) Money.ZERO else parseAmount(text)?.takeIf { it >= Money.ZERO }

/** Saving stamps [today] as the update date. Deposits and fees may stay empty (zero); value may not. */
fun buildFund(draft: FundDraft, today: LocalDate, newId: () -> String): FundResult {
    if (draft.provider.isBlank()) return FundResult.Invalid(FundField.PROVIDER)
    val value = amount(draft.value, required = true) ?: return FundResult.Invalid(FundField.VALUE)
    val deposits = amount(draft.totalDeposits, required = false) ?: return FundResult.Invalid(FundField.DEPOSITS)
    val monthly = amount(draft.monthlyDeposit, required = false) ?: return FundResult.Invalid(FundField.MONTHLY)
    val onBalance = fee(draft.feeOnBalance) ?: return FundResult.Invalid(FundField.FEE_BALANCE)
    val onDeposits = fee(draft.feeOnDeposits) ?: return FundResult.Invalid(FundField.FEE_DEPOSITS)
    return FundResult.Ok(
        PensionFund(
            draft.id ?: newId(), draft.kind, draft.provider.trim(), value, deposits, monthly, onBalance, onDeposits,
            draft.openedOn, today,
        )
    )
}
