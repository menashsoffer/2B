package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.domain.MonthSnapshot
import com.menash.myself.feature.money.importer.ImportFixtures
import com.menash.myself.feature.money.importer.RowStatus
import com.menash.myself.feature.money.importer.StoredTransaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** The month headline: plan only before imports, "נשאר לשוטפות" once lines exist, the frozen result once closed. */
class MonthStatusTest {

    private val november = YearMonth.of(2026, 11)
    private val october = YearMonth.of(2026, 10)

    private fun view(month: YearMonth, lines: List<StoredTransaction> = emptyList(), data: com.menash.myself.feature.money.data.MoneyData = MoneySamples.filled) =
        buildMoneyMonth(data, month, month.atDay(5), MoneySamples.rate, lines).view as MonthView.Planned

    private fun lines(name: String) = ImportFixtures.parse(name).rows.map { StoredTransaction(it) }

    @Test
    fun usageThresholds() {
        assertEquals(UsageLevel.NORMAL, usageLevel(Money(79), Money(100)))
        assertEquals(UsageLevel.NEAR, usageLevel(Money(80), Money(100)))
        assertEquals(UsageLevel.NEAR, usageLevel(Money(100), Money(100)))
        assertEquals(UsageLevel.OVER, usageLevel(Money(101), Money(100)))
        assertEquals(UsageLevel.OVER, usageLevel(Money(1), Money.ZERO))
        assertEquals(UsageLevel.NORMAL, usageLevel(Money.ZERO, Money.ZERO))
    }

    @Test
    fun noImport_isThePlansSurplus() {
        val planned = view(october)
        assertEquals(MonthStatus.PlanOnly(planned.plan.expectedSurplus), planned.status)
    }

    @Test
    fun imported_leadsWithWhatIsLeftForCurrent() {
        val stored = lines("max_nov.xlsx").map { if (it.row.description == "שופרסל דיל 123") it.copy(categoryId = "current-1") else it }
        val planned = view(november, stored)
        val current = planned.rows.single { it.kind == MonthRowKind.CURRENT }
        val status = planned.status as MonthStatus.Open
        assertEquals(current.total, status.currentBudget)
        assertEquals(Money(25_430), status.currentSpent)
        assertEquals(Money(current.total.agorot - 25_430), status.leftForCurrent)
        assertEquals(planned.plan.expectedSurplus, status.expectedSurplus)
        assertEquals(Money(63_230), status.spentSoFar)
        val latest = stored.filter { it.row.billingMonth == november && it.row.status == RowStatus.COUNTED }.maxOf { it.row.date }
        assertEquals(latest, status.dataUntil)
    }

    @Test
    fun closedMonth_showsTheFrozenResult() {
        val close = ClosedMonth(
            october, MonthSnapshot(october, Money(1_000_000), Money(870_000)), emptyMap(), emptyMap(), emptyMap(), emptyMap(),
            LocalDate.of(2026, 11, 3),
        )
        val planned = view(october, lines("max_nov.xlsx"), MoneySamples.filled.copy(closedMonths = listOf(close)))
        assertEquals(MonthStatus.Closed(Money(130_000), Money(1_000_000), Money(870_000), LocalDate.of(2026, 11, 3)), planned.status)
    }
}
