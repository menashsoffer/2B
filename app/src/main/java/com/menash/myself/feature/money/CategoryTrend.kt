package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.AmountChange
import com.menash.myself.feature.money.domain.AmountHistory
import com.menash.myself.feature.money.domain.CategoryBaseline
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.Frequency
import com.menash.myself.feature.money.domain.MonthRange
import com.menash.myself.feature.money.domain.SuggestionStatus
import com.menash.myself.feature.money.domain.VariableBudget
import com.menash.myself.feature.money.domain.amountFor
import com.menash.myself.feature.money.domain.categoryBaseline
import com.menash.myself.feature.money.domain.suggestPlan
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/** One month of the trend. [actual] is null when the month is not closed: never drawn as zero. */
data class TrendMonth(val month: YearMonth, val actual: Money?, val planned: Money?)

/** Where a new plan amount for the category goes. */
sealed interface PlanTarget {
    /** One monthly ILS item: its amount changes from this month on. */
    data class Item(val entry: MoneyEntry, val name: String, val current: Money) : PlanTarget
    /** No item yet, and the category is "שוטפות": a new monthly budget with the category's name. */
    data object NewBudget : PlanTarget
    /** Several items, or one the plan cannot change in one tap (yearly, USD): changed in its own editor. */
    data class Manual(val count: Int) : PlanTarget
}

data class CategoryTrendView(
    val categoryId: String,
    val name: String,
    val klass: ExpenseClass,
    val months: List<TrendMonth>,
    /** Over the closed months of the window; null when none is closed. */
    val baseline: CategoryBaseline?,
    val closedCount: Int,
    /** This month's plan for the category; null when nothing is planned. */
    val currentPlan: Money?,
    /** The suggested monthly plan, when the closed months suggest a change. */
    val suggested: Money?,
    val status: SuggestionStatus?,
    val target: PlanTarget?,
    /** The month a one-tap change starts from: this month. */
    val from: YearMonth,
)

private const val TrendMonths = 12

private val Changes = setOf(SuggestionStatus.UNDER_PLANNED, SuggestionStatus.OVER_PLANNED, SuggestionStatus.NO_PLAN, SuggestionStatus.UNUSED)

/** Monthly ILS fixed expenses and variable budgets can take a new amount; anything else is changed by hand. */
internal fun planTarget(data: MoneyData, categoryId: String, klass: ExpenseClass, month: YearMonth): PlanTarget {
    val fixed = data.items.fixedExpenses.filter { it.categoryId == categoryId }
    val variable = data.items.variableBudgets.filter { it.categoryId == categoryId }
    val subscriptions = data.items.subscriptions.filter { it.categoryId == categoryId }
    val count = fixed.size + variable.size + subscriptions.size
    return when {
        count == 0 -> if (klass == ExpenseClass.CURRENT) PlanTarget.NewBudget else PlanTarget.Manual(0)
        count > 1 || subscriptions.isNotEmpty() -> PlanTarget.Manual(count)
        variable.size == 1 -> variable.single().let { PlanTarget.Item(MoneyEntry.VariableBudgetEntry(it), it.name, amountFor(it, month) ?: Money.ZERO) }
        fixed.single().frequency == Frequency.MONTHLY ->
            fixed.single().let { PlanTarget.Item(MoneyEntry.FixedExpenseEntry(it), it.name, amountFor(it, month) ?: Money.ZERO) }
        else -> PlanTarget.Manual(1)
    }
}

/** The entry that sets the category's plan to [amount] from [month] on; null for [PlanTarget.Manual]. */
fun planChange(target: PlanTarget, view: CategoryTrendView, amount: Money, newId: () -> String): MoneyEntry? = when (target) {
    is PlanTarget.Manual -> null
    PlanTarget.NewBudget -> MoneyEntry.VariableBudgetEntry(
        VariableBudget(newId(), view.name, AmountHistory(listOf(AmountChange(view.from, amount))), categoryId = view.categoryId),
    )
    is PlanTarget.Item -> when (val entry = target.entry) {
        is MoneyEntry.VariableBudgetEntry ->
            MoneyEntry.VariableBudgetEntry(entry.value.copy(amounts = withAmount(entry.value.amounts, view.from, amount, AmountScope.FROM_NOW)))
        is MoneyEntry.FixedExpenseEntry ->
            MoneyEntry.FixedExpenseEntry(entry.value.copy(amounts = withAmount(entry.value.amounts, view.from, amount, AmountScope.FROM_NOW)))
        else -> null
    }
}

/**
 * The last twelve months up to last month, closed months only, against each month's plan. The suggestion is the
 * calibration engine's (six closed months, three at least) for this category alone. Null for an unknown category.
 */
fun categoryTrend(data: MoneyData, categoryId: String, today: LocalDate, rate: BigDecimal?): CategoryTrendView? {
    val category = data.categories.firstOrNull { it.id == categoryId } ?: return null
    val current = YearMonth.from(today)
    val range = MonthRange.last(current.minusMonths(1), TrendMonths)
    val closed = data.closedMonths.associateBy { it.month }
    val plans = plansFor(data, range.months + current, rate)
    val months = range.months.map { month ->
        TrendMonth(month, closed[month]?.let { it.byCategory[categoryId] ?: Money.ZERO }, plans[month]?.get(categoryId)?.takeIf { it.isPositive() })
    }
    val inWindow = data.closedMonths.filter { it.month in range.months }
    val baseline = categoryBaseline(data.closedMonths, range.to)
    val row = baseline.rows.firstOrNull { it.categoryId == categoryId }
    val currentPlan = plans[current]?.get(categoryId)?.takeIf { it.isPositive() }
    val suggestion = suggestPlan(baseline, currentPlan?.let { mapOf(categoryId to it) } ?: emptyMap(), data.categories)
        .firstOrNull { it.categoryId == categoryId }
    val suggested = suggestion?.takeIf { it.status in Changes }?.suggested
    return CategoryTrendView(
        categoryId = categoryId,
        name = category.name,
        klass = category.klass,
        months = months,
        baseline = if (inWindow.isEmpty()) null else row,
        closedCount = inWindow.size,
        currentPlan = currentPlan,
        suggested = suggested,
        status = suggestion?.status,
        target = if (category.klass == ExpenseClass.PERIODIC) null else planTarget(data, categoryId, category.klass, current),
        from = current,
    )
}
