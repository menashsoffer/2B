package com.menash.myself.e2e.stories

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.*
import com.menash.myself.feature.money.importer.*
import java.time.LocalDate
import java.time.YearMonth

/** Fictional, deterministic fixtures. IDs are stable so a partially completed seed can be retried. */
data class Story(val entries: List<MoneyEntry>, val rows: List<StoredTransaction>)

object Stories {
    val names = setOf("plan_only", "mid_month", "ready_to_close", "history", "no_current_budget")
    const val INCOME_ID = "e2e-story-income"
    private fun ils(n: Long) = Money.fromShekels(n)

    fun build(name: String, today: LocalDate): Story {
        require(name in names) { "Unknown story: $name" }
        val month = YearMonth.from(today)
        val start = month.minusMonths(12)
        fun amount(n: Long) = AmountHistory(listOf(AmountChange(start, ils(n))))
        val categories = listOf(
            Category("story-rent", "שכר דירה", ExpenseClass.FIXED),
            Category("story-electricity", "חשמל", ExpenseClass.FIXED),
            Category("story-food", "מזון", ExpenseClass.CURRENT),
            Category("story-fuel", "דלק", ExpenseClass.CURRENT),
            Category("story-insurance", "ביטוח רכב", ExpenseClass.PERIODIC),
        )
        val noBudget = name == "no_current_budget"
        val income = Income(INCOME_ID, "משכורת", amount(if (noBudget) 26400 else 20000))
        val fixed = if (noBudget) listOf(FixedExpense("story-rent-item", "הוצאה קבועה", amount(157), categoryId = "story-rent")) else listOf(
            FixedExpense("story-rent-item", "שכר דירה", amount(5500), categoryId = "story-rent"),
            FixedExpense("story-electricity-item", "חשמל", amount(500), categoryId = "story-electricity"),
        )
        val variable = if (noBudget) emptyList() else listOf(
            VariableBudget("story-food-item", "מזון", amount(3000), categoryId = "story-food"),
            VariableBudget("story-fuel-item", "דלק", amount(2000), categoryId = "story-fuel"),
        )
        val periodic = if (noBudget) emptyList() else listOf(PeriodicBudget("story-insurance-item", "story-insurance", "ביטוח רכב", ils(1200), start))
        val items = MoneyItems(incomes = listOf(income), fixedExpenses = fixed, variableBudgets = variable, periodicBudgets = periodic)
        val rows = mutableListOf<StoredTransaction>()
        fun row(m: YearMonth, suffix: String, source: Source, charge: Long, category: String?, description: String) {
            val date = m.atDay(5)
            rows += StoredTransaction(StatementRow("story-$m-$suffix", source, "1234", date, date, description, charge = ils(charge)), category)
        }
        fun monthRows(m: YearMonth, count: Int, expense: Long) {
            row(m, "salary", Source.HAPOALIM, -20000, null, "משכורת")
            repeat(count) { i -> row(m, "card-$i", Source.MAX, expense, if (i < count / 2) "story-food" else "story-fuel", if (i < count / 2) "חנות מזון" else "תחנת דלק") }
        }
        when (name) {
            "mid_month" -> monthRows(month, 20, 160)
            "ready_to_close" -> monthRows(month.minusMonths(1), 20, 350)
            "history" -> (1L..6L).forEach { monthRows(month.minusMonths(it), 20, 160) }
            "no_current_budget" -> {
                row(month, "fixed", Source.MAX, 1012, "story-rent", "הוצאה קבועה")
                row(month, "unassigned", Source.MAX, 6120, null, "רכישה כללית")
            }
        }
        val closed = if (name == "history") (3L..6L).map { offset ->
            val target = month.minusMonths(offset)
            val balances = if (offset == 5L || offset == 6L) BalancesAtClose.Entered(mapOf("story-bank" to ils(30000)), emptyMap()) else BalancesAtClose.Unknown
            closeMonth(target, items, toTransactions(rows), actualIncome(rows, target), balances, null, today)
        } else emptyList()
        return Story(
            categories.map { MoneyEntry.CategoryEntry(it) } + fixed.map { MoneyEntry.FixedExpenseEntry(it) } +
                variable.map { MoneyEntry.VariableBudgetEntry(it) } + periodic.map { MoneyEntry.PeriodicBudgetEntry(it) } +
                closed.map { MoneyEntry.ClosedMonthEntry(it) } + MoneyEntry.IncomeEntry(income), rows,
        )
    }
}
