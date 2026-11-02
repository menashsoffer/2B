package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.MonthRange
import com.menash.myself.feature.money.domain.MonthSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** The period screen: presets end at last month, and every figure comes from the period engine over closed months. */
class PeriodViewTest {

    private fun ym(month: Int) = YearMonth.of(2026, month)

    private fun close(month: Int, income: Long, expense: Long, byCategory: Map<String?, Money>, balances: Map<String, Money> = emptyMap()) =
        ClosedMonth(ym(month), MonthSnapshot(ym(month), Money(income), Money(expense)), byCategory, emptyMap(), balances, emptyMap(), ym(month + 1).atDay(2))

    @Test
    fun presetsEndAtLastMonth() {
        val today = LocalDate.of(2026, 11, 2)
        assertEquals(MonthRange(ym(8), ym(10)), periodRange(PeriodPreset.LAST_3, today))
        assertEquals(MonthRange(ym(5), ym(10)), periodRange(PeriodPreset.LAST_6, today))
        assertEquals(MonthRange(ym(1), ym(10)), periodRange(PeriodPreset.THIS_YEAR, today))
        assertEquals(MonthRange(YearMonth.of(2025, 1), YearMonth.of(2025, 12)), periodRange(PeriodPreset.LAST_YEAR, today))
        val january = LocalDate.of(2027, 1, 10)
        assertEquals(MonthRange(YearMonth.of(2027, 1), YearMonth.of(2027, 1)), periodRange(PeriodPreset.THIS_YEAR, january))
        assertEquals(MonthRange(YearMonth.of(2026, 10), YearMonth.of(2026, 12)), periodRange(PeriodPreset.LAST_3, january))
    }

    @Test
    fun noClosedMonth_isEmpty() {
        val view = periodView(MoneySamples.filled, PeriodPreset.THIS_YEAR, MoneySamples.today, MoneySamples.rate)
        assertNull(view.summary)
        assertTrue(view.categories.isEmpty())
    }

    @Test
    fun closedMonths_classesCategoriesAndPlan() {
        val data = MoneySamples.filled.copy(
            closedMonths = listOf(
                close(8, 1_800_000, 1_000_000, mapOf("fixed-3" to Money(520_000), "current-1" to Money(330_000), null to Money(10_000))),
                close(9, 1_800_000, 900_000, mapOf("fixed-3" to Money(520_000), "current-1" to Money(280_000)), mapOf("a" to Money(5_000_000))),
            ),
        )
        val view = periodView(data, PeriodPreset.LAST_3, LocalDate.of(2026, 11, 2), MoneySamples.rate)
        val summary = view.summary!!
        assertEquals(listOf(ym(10)), summary.missingMonths)
        assertEquals(Money(1_700_000), summary.saving)
        assertEquals(listOf(ExpenseClass.FIXED to Money(1_040_000), ExpenseClass.CURRENT to Money(610_000), null to Money(10_000)), view.classes)
        assertEquals(listOf("fixed-3", "current-1", null), view.categories.map { it.categoryId })
        val food = view.categories.single { it.categoryId == "current-1" }
        assertEquals(Money(600_000), food.planned) // ₪3,000 a month, two closed months
        assertEquals(Money(10_000), food.diff)
        assertEquals(Money(5_000_000), view.netWorth!!.end)
    }

    @Test
    fun calendarYear_comparesWithYearBefore_byMonthlyAverage() {
        fun closeIn(year: Int, month: Int, expense: Long, food: Long) = YearMonth.of(year, month).let { m ->
            ClosedMonth(m, MonthSnapshot(m, Money(1_800_000), Money(expense)), mapOf("current-1" to Money(food)), emptyMap(), emptyMap(), emptyMap(), m.plusMonths(1).atDay(2))
        }
        val data = MoneySamples.filled.copy(
            closedMonths = listOf(
                closeIn(2025, 11, 1_200_000, 280_000), closeIn(2025, 12, 1_400_000, 300_000),
                closeIn(2026, 9, 1_300_000, 330_000), closeIn(2026, 10, 1_500_000, 350_000),
            ),
        )
        val view = periodView(data, PeriodPreset.THIS_YEAR, LocalDate.of(2026, 11, 2), MoneySamples.rate)
        val comparison = view.comparison!!
        assertEquals(Money(1_400_000), comparison.expenses.thisYear)
        assertEquals(Money(1_300_000), comparison.expenses.previousYear)
        assertEquals(Money(100_000), comparison.expenses.difference)
        assertEquals("current-1", view.risers.single().categoryId)
        assertEquals(Money(50_000), view.risers.single().difference)
        assertNull(periodView(data, PeriodPreset.LAST_3, LocalDate.of(2026, 11, 2), MoneySamples.rate).comparison)
    }

    @Test
    fun january_opensOnLastYear() {
        assertEquals(PeriodPreset.LAST_YEAR, defaultPreset(LocalDate.of(2027, 1, 5)))
        assertEquals(PeriodPreset.THIS_YEAR, defaultPreset(LocalDate.of(2026, 11, 2)))
    }
}
