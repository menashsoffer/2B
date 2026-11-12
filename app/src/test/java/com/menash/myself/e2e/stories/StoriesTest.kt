package com.menash.myself.e2e.stories

import com.menash.myself.e2e.stories.Stories
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.importer.actualIncome
import com.menash.myself.feature.money.importer.toTransactions
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class StoriesTest {
    private val today = LocalDate.of(2026, 11, 3)
    private val month = YearMonth.from(today)

    @Test fun buildsEveryStoryWithExpectedTotals() {
        val stories = Stories.names.associateWith { Stories.build(it, today) }
        assertEquals(0, stories.getValue("plan_only").rows.size)
        val mid = stories.getValue("mid_month")
        assertEquals(21, mid.rows.size)
        assertEquals(320000L, toTransactions(mid.rows).sumOf { it.amount.agorot })
        assertEquals(2000000L, actualIncome(mid.rows, month).agorot)
        val ready = stories.getValue("ready_to_close")
        assertEquals(700000L, toTransactions(ready.rows).sumOf { it.amount.agorot })
        val history = stories.getValue("history")
        assertEquals(126, history.rows.size)
        assertEquals(4, history.entries.filterIsInstance<MoneyEntry.ClosedMonthEntry>().size)
        val noBudget = stories.getValue("no_current_budget")
        assertEquals(0, noBudget.entries.filterIsInstance<MoneyEntry.VariableBudgetEntry>().size)
        assertEquals(713200L, toTransactions(noBudget.rows).sumOf { it.amount.agorot })
    }
}
