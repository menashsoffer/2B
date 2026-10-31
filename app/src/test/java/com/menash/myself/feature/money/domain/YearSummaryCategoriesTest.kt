package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class YearSummaryCategoriesTest {
    @Test fun plansActualsAndClasses() {
        val jan = YearMonth.of(2026, 1)
        val mar = jan.plusMonths(2)
        val close = ClosedMonth(jan, MonthSnapshot(jan, Money(0), Money(100)),
            mapOf("over" to Money(80), "under" to Money(20), null to Money(5), "unknown" to Money(10)),
            emptyMap(), emptyMap(), emptyMap(), jan.atEndOfMonth())
        val rows = categoryYear(listOf(close), 2026, mapOf(jan to mapOf("over" to Money(40), "under" to Money(30), "planned" to Money(5)), mar to mapOf("over" to Money(1000))))
        assertEquals(listOf("over", "planned", "under", "unknown", null), rows.map { it.categoryId })
        assertEquals(Money(40), rows[0].diff)
        assertEquals(Money.ZERO, rows[1].actual)
        assertNull(rows[3].planned)
        val classes = classYear(rows, listOf(Category("over", "parent", ExpenseClass.FIXED), Category("under", "child", ExpenseClass.CURRENT, "over")))
        assertEquals(Money(80), classes[ExpenseClass.FIXED])
        assertEquals(Money(20), classes[ExpenseClass.CURRENT])
        assertEquals(Money(15), classes[null])
    }
}
