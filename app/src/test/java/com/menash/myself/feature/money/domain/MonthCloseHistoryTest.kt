package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class MonthCloseHistoryTest {
    private fun closed(month: Int, expense: Long, income: Long = 1000): ClosedMonth {
        val m = YearMonth.of(2026, month)
        return ClosedMonth(m, MonthSnapshot(m, Money(income), Money(expense)), emptyMap(), emptyMap(), emptyMap(), emptyMap(), m.atEndOfMonth().plusDays(1))
    }

    @Test fun octoberReplacementWinsAndSorts() {
        val old = closed(10, 10)
        val replacement = closed(10, 20)
        val november = closed(11, 30)
        val september = closed(9, 40)
        val input = mutableListOf(november, old, september)
        assertEquals(listOf(september, replacement, november), upsert(input, replacement))
        assertEquals(listOf(november, old, september), input)
    }

    @Test fun lastExistingDuplicateWinsOtherMonths() {
        val lastSeptember = closed(9, 99)
        val new = closed(10, 55)
        val existing = listOf(closed(10, 10), closed(9, 10), lastSeptember, closed(10, 20))
        assertEquals(listOf(lastSeptember, new), upsert(existing, new))
        assertEquals(listOf(new), upsert(emptyList(), new))
    }

    @Test fun averagesZeroOneTwoThreeAndFiveClosedMonths() {
        val rows = listOf(closed(5, 500), closed(1, 100), closed(4, 400), closed(2, 200), closed(3, 300))
        assertNull(averageMonthlyExpenses(emptyList()))
        assertEquals(Money(500), averageMonthlyExpenses(rows.take(1)))
        assertEquals(Money(300), averageMonthlyExpenses(rows.take(2)))
        assertEquals(Money(333), averageMonthlyExpenses(rows.take(3)))
        assertEquals(Money(400), averageMonthlyExpenses(rows))
    }

    @Test fun calendarGapsAddNoZeroMonths() {
        assertEquals(Money(200), averageMonthlyExpenses(listOf(closed(1, 100), closed(10, 300))))
    }

    @Test fun roundsHalfAgoraUp() {
        assertEquals(Money(2), averageMonthlyExpenses(listOf(closed(1, 1), closed(2, 2))))
    }

    @Test fun rejectsDuplicateMonthsEvenOutsideLatestThree() {
        val input = listOf(closed(1, 1), closed(1, 2), closed(3, 3), closed(4, 4), closed(5, 5))
        assertThrows(IllegalArgumentException::class.java) { averageMonthlyExpenses(input) }
        assertThrows(IllegalArgumentException::class.java) { monthlySaving(input) }
    }

    @Test fun savingDelegatesIncludingNegativeSavingRoundingAndLatestThree() {
        val input = listOf(closed(1, 0), closed(3, 1002), closed(8, 1001), closed(12, 1002))
        assertEquals(savingRate(input.map { it.snapshot }), monthlySaving(input))
        assertEquals(Money(-2), monthlySaving(input))
        assertEquals(Money(-2), monthlySaving(input.takeLast(2)))
        assertEquals(Money(-2), monthlySaving(input.takeLast(1)))
        assertNull(monthlySaving(emptyList()))
    }

    @Test fun expenseSummationIsChecked() {
        assertThrows(ArithmeticException::class.java) {
            averageMonthlyExpenses(listOf(closed(1, Long.MAX_VALUE), closed(2, 1)))
        }
    }
}
