package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.MonthSnapshot
import com.menash.myself.feature.money.domain.PeriodicStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.YearMonth

/** "סיכום החודש": read from the frozen close, compared with earlier closes and today's plan. */
class MonthSummaryTest {

    private fun ym(month: Int) = YearMonth.of(2026, month)

    private fun close(
        month: Int, income: Long, expense: Long, food: Long,
        balances: Map<String, Money> = emptyMap(), periodic: Map<String, PeriodicStatus> = emptyMap(),
    ) = ClosedMonth(
        ym(month), MonthSnapshot(ym(month), Money(income), Money(expense)),
        mapOf("fixed-3" to Money(520_000), "current-1" to Money(food)), periodic, balances, emptyMap(), ym(month + 1).atDay(2),
    )

    @Test
    fun notClosed_isNull() {
        assertNull(monthSummary(MoneySamples.filled, ym(10), MoneySamples.rate))
    }

    @Test
    fun firstClose_hasNoComparisonAndNoSuggestions() {
        val data = MoneySamples.filled.copy(closedMonths = listOf(close(10, 1_800_000, 1_300_000, 340_000)))
        val view = monthSummary(data, ym(10), MoneySamples.rate)!!
        assertEquals(Money(500_000), view.left)
        assertEquals(BigDecimal("27.8"), view.leftPercent)
        assertNull(view.previousAverage)
        assertNull(view.vsAverage)
        assertNull(view.suggestions)
        assertNull(view.netWorth)
    }

    @Test
    fun classesAndOverruns_againstTodaysPlan() {
        val data = MoneySamples.filled.copy(closedMonths = listOf(close(10, 1_800_000, 1_300_000, 340_000)))
        val view = monthSummary(data, ym(10), MoneySamples.rate)!!
        val current = view.classes.single { it.klass == ExpenseClass.CURRENT }
        assertEquals(Money(340_000), current.actual)
        val food = view.overruns.single()
        assertEquals("current-1", food.categoryId)
        assertEquals(Money(300_000), food.planned)
        assertEquals(Money(40_000), food.over)
    }

    @Test
    fun comparesWithUpToThreeEarlierCloses() {
        val data = MoneySamples.filled.copy(
            closedMonths = listOf(
                close(6, 1_800_000, 1_800_000, 300_000), // left 0, outside the three
                close(7, 1_800_000, 1_400_000, 300_000), // 400,000
                close(8, 1_800_000, 1_500_000, 300_000), // 300,000
                close(9, 1_800_000, 1_300_000, 300_000), // 500,000
                close(10, 1_800_000, 1_200_000, 300_000), // 600,000
            ),
        )
        val view = monthSummary(data, ym(10), MoneySamples.rate)!!
        assertEquals(3, view.previousCount)
        assertEquals(Money(400_000), view.previousAverage)
        assertEquals(Money(200_000), view.vsAverage)
        assertEquals(5, view.suggestionMonths)
    }

    @Test
    fun suggestionsAfterThreeCloses_andNetWorthFromLastKnownClose() {
        val data = MoneySamples.filled.copy(
            closedMonths = listOf(
                close(8, 1_800_000, 1_300_000, 420_000, balances = mapOf("a" to Money(5_000_000))),
                close(9, 1_800_000, 1_300_000, 420_000),
                close(10, 1_800_000, 1_300_000, 420_000, balances = mapOf("a" to Money(5_400_000))),
            ),
        )
        val view = monthSummary(data, ym(10), MoneySamples.rate)!!
        val food = view.suggestions!!.single { it.categoryId == "current-1" }
        assertEquals(Money(300_000), food.current)
        assertEquals(Money(420_000), food.suggested)
        assertEquals(ym(8), view.netWorth!!.startFrom)
        assertEquals(Money(400_000), view.netWorth!!.change)
    }

    @Test
    fun periodicFundsByName() {
        val status = PeriodicStatus(Money(400_000), Money(500_000), Money(0))
        val data = MoneySamples.filled.copy(closedMonths = listOf(close(10, 1_800_000, 1_300_000, 300_000, periodic = mapOf("per-holidays" to status, "gone" to status))))
        val view = monthSummary(data, ym(10), MoneySamples.rate)!!
        assertEquals(listOf("יהדות / חגים"), view.periodic.map { it.name })
        assertEquals(Money(100_000), view.periodic.single().status.advance)
    }
}
