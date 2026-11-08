package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.AmountChange
import com.menash.myself.feature.money.domain.AmountHistory
import com.menash.myself.feature.money.domain.Category
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.FixedExpense
import com.menash.myself.feature.money.domain.Frequency
import com.menash.myself.feature.money.domain.Income
import com.menash.myself.feature.money.domain.PeriodicBudget
import com.menash.myself.feature.money.domain.VEHICLE_ENERGY_CATEGORY_NAME
import com.menash.myself.feature.money.domain.VariableBudget
import com.menash.myself.feature.money.domain.amountFor
import com.menash.myself.feature.money.domain.defaultCategories
import com.menash.myself.feature.money.domain.newCategory
import com.menash.myself.feature.money.importer.PaamonimLine
import com.menash.myself.feature.money.importer.PaamonimPlan
import com.menash.myself.feature.money.importer.PlanGroup
import java.time.Month
import java.time.YearMonth

/** How one line of the file relates to the plan already stored. */
enum class PlanLineStatus {
    /** No plan item for this category or income yet; one is added. */
    NEW,
    /** The category does not exist either (a line the household named itself); both are added. */
    NEW_CATEGORY,
    /** One plan item exists with another amount; it changes from the import month on. */
    CHANGED,
    /** Already in the plan with the same monthly amount (within ₪1). Nothing to do. */
    SAME,
    /** More than one plan item shares the category or income name; the app does not guess which to change. */
    SPLIT,
    /** The "מנויים" total, while subscriptions are already listed one by one. Adding it would count them twice. */
    SUBSCRIPTIONS,
    /**
     * An annual budget whose year already runs in open months before the import month. It has one annual amount
     * and no history, so changing it would change those months too; it is left for the household to update.
     */
    PERIODIC_IN_USE,
    /** Two lines of the source name the same category or income; neither is guessed. */
    DUPLICATE,
}

/** One line of the draft. [entries] are written when the line is selected; [current] is today's monthly amount. */
data class PlanImportLine(
    val key: String,
    val group: PlanGroup,
    val name: String,
    val monthly: Money,
    val status: PlanLineStatus,
    val current: Money? = null,
    val entries: List<MoneyEntry> = emptyList(),
) {
    val selectable: Boolean get() = status == PlanLineStatus.NEW || status == PlanLineStatus.NEW_CATEGORY || status == PlanLineStatus.CHANGED
}

/** Where a draft comes from: a Paamonim file's budget month, or the household's own recent months. */
sealed interface PlanBasis {
    data class PaamonimFile(val month: Month) : PlanBasis
    data class History(val months: List<YearMonth>) : PlanBasis
}

/**
 * The file's plan against the stored one. Nothing is written until the household confirms; every change starts
 * at [from] (the month of the import), so closed months and earlier plans stay as they were.
 */
data class PlanImportDraft(
    val basis: PlanBasis,
    val from: YearMonth,
    val lines: List<PlanImportLine>,
    val debtRepayment: Money?,
    /** The household has no categories yet: the Paamonim defaults are written with the first save. */
    val seedCategories: List<Category> = emptyList(),
) {
    val selectableKeys: Set<String> get() = lines.filter { it.selectable }.map { it.key }.toSet()

    /** What one confirm writes: the default categories when missing, then the selected lines. */
    fun toSave(selected: Set<String>): List<MoneyEntry> {
        val chosen = lines.filter { it.selectable && it.key in selected }
        if (chosen.isEmpty()) return emptyList()
        return seedCategories.map { MoneyEntry.CategoryEntry(it) } + chosen.flatMap { it.entries }
    }
}

private const val SAME_WITHIN_AGOROT = 100L
private const val SUBSCRIPTIONS_NAME = "מנויים"
private const val FILE_FUEL_NAME = "דלק"

fun draftPlanImport(plan: PaamonimPlan, data: MoneyData, from: YearMonth, newId: () -> String): PlanImportDraft =
    draftLines(PlanBasis.PaamonimFile(plan.month), plan.lines, plan.debtRepayment, data, from, newId)

/** The same review for a plan built from the household's own months; null with too little history. */
fun draftPlanFromHistory(history: List<SpendingMonth>, data: MoneyData, from: YearMonth, newId: () -> String): PlanImportDraft? =
    planFromHistory(history, data)?.let { lines -> draftLines(PlanBasis.History(history.map { it.month }), lines, null, data, from, newId) }

private fun draftLines(
    basis: PlanBasis, source: List<PaamonimLine>, debts: Money?, data: MoneyData, from: YearMonth, newId: () -> String,
): PlanImportDraft {
    val seed = if (data.categories.isEmpty()) defaultCategories() else emptyList()
    val categories = (data.categories + seed).toMutableList()
    // Two source lines for one category or income would save two items for it; both are shown and left alone.
    val targets = source.map(::target)
    val repeated = targets.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    val lines = source.mapIndexed { index, line ->
        // The index keeps every source line apart, even when the file repeats a name.
        val key = "$index:${line.group}:${line.name}"
        when {
            targets[index] in repeated -> PlanImportLine(key, line.group, line.name, line.monthly, PlanLineStatus.DUPLICATE)
            line.group == PlanGroup.INCOME -> incomeLine(key, line, data, from, newId)
            else -> expenseLine(key, line, data, categories, from, newId)
        }
    }
    return PlanImportDraft(basis, from, lines, debts, seed)
}

/** What a source line is matched to: an income name, or a category name within its class. */
private fun target(line: PaamonimLine): String =
    if (line.group == PlanGroup.INCOME) "income:${normal(line.name)}" else "${klassOf(line.group)}:${normal(categoryName(line))}"

private fun categoryName(line: PaamonimLine): String =
    if (line.group == PlanGroup.CURRENT && normal(line.name) == FILE_FUEL_NAME) VEHICLE_ENERGY_CATEGORY_NAME else line.name

private fun same(a: Money, b: Money) = kotlin.math.abs(a.agorot - b.agorot) <= SAME_WITHIN_AGOROT

private fun normal(name: String) = name.trim().replace(Regex("""\s+"""), " ")

/** The amount from [month] on. A "this month only" override of [month] would hide it, so it goes; later overrides stay. */
private fun AmountHistory.from(month: YearMonth, amount: Money) =
    copy(changes = changes.filterNot { it.effectiveFrom == month } + AmountChange(month, amount), overrides = overrides - month)

private fun klassOf(group: PlanGroup) = when (group) {
    PlanGroup.FIXED -> ExpenseClass.FIXED
    PlanGroup.PERIODIC -> ExpenseClass.PERIODIC
    else -> ExpenseClass.CURRENT
}

private fun incomeLine(key: String, line: PaamonimLine, data: MoneyData, from: YearMonth, newId: () -> String): PlanImportLine {
    val matches = data.items.incomes.filter { normal(it.name) == normal(line.name) }
    return when (matches.size) {
        0 -> PlanImportLine(key, line.group, line.name, line.monthly, PlanLineStatus.NEW,
            entries = listOf(MoneyEntry.IncomeEntry(Income(newId(), line.name, AmountHistory(listOf(AmountChange(from, line.monthly)))))))
        1 -> {
            val income = matches.single()
            val current = amountFor(income, from) ?: Money.ZERO
            if (same(current, line.monthly)) PlanImportLine(key, line.group, line.name, line.monthly, PlanLineStatus.SAME, current)
            else PlanImportLine(key, line.group, line.name, line.monthly, PlanLineStatus.CHANGED, current,
                listOf(MoneyEntry.IncomeEntry(income.copy(amounts = income.amounts.from(from, line.monthly)))))
        }
        else -> PlanImportLine(key, line.group, line.name, line.monthly, PlanLineStatus.SPLIT,
            Money(matches.sumOf { (amountFor(it, from) ?: Money.ZERO).agorot }))
    }
}

private fun expenseLine(
    key: String, line: PaamonimLine, data: MoneyData, categories: MutableList<Category>, from: YearMonth, newId: () -> String,
): PlanImportLine {
    val klass = klassOf(line.group)
    val wanted = categoryName(line)
    val existing = categories.firstOrNull { it.klass == klass && it.parentId == null && normal(it.name) == normal(wanted) }
    if (existing == null) {
        val category = newCategory(line.name, klass, categories, newId)
            ?: return PlanImportLine(key, line.group, line.name, line.monthly, PlanLineStatus.SPLIT)
        categories += category
        return PlanImportLine(key, line.group, line.name, line.monthly, PlanLineStatus.NEW_CATEGORY,
            entries = listOf(MoneyEntry.CategoryEntry(category), newItem(line, category, from, newId)))
    }
    val items = data.items
    if (normal(existing.name) == SUBSCRIPTIONS_NAME && items.subscriptions.isNotEmpty()) {
        return PlanImportLine(key, line.group, line.name, line.monthly, PlanLineStatus.SUBSCRIPTIONS)
    }
    val fixed = items.fixedExpenses.filter { it.categoryId == existing.id }
    val variable = items.variableBudgets.filter { it.categoryId == existing.id }
    val periodic = items.periodicBudgets.filter { it.categoryId == existing.id }
    val monthlyNow = fixed.sumOf { monthlyOf(it, from).agorot } + variable.sumOf { (amountFor(it, from) ?: Money.ZERO).agorot } +
        periodic.sumOf { (it.annualAmount / 12).agorot }
    val current = Money(monthlyNow)
    val count = fixed.size + variable.size + periodic.size
    return when {
        count == 0 -> PlanImportLine(key, line.group, line.name, line.monthly, PlanLineStatus.NEW,
            entries = listOf(newItem(line, existing, from, newId)))
        count > 1 -> PlanImportLine(key, line.group, line.name, line.monthly, PlanLineStatus.SPLIT, current)
        same(current, line.monthly) -> PlanImportLine(key, line.group, line.name, line.monthly, PlanLineStatus.SAME, current)
        periodic.isNotEmpty() && !yearIsClosedBefore(periodic.single(), from, data) ->
            PlanImportLine(key, line.group, line.name, line.monthly, PlanLineStatus.PERIODIC_IN_USE, current)
        else -> PlanImportLine(key, line.group, line.name, line.monthly, PlanLineStatus.CHANGED, current, listOf(
            fixed.singleOrNull()?.let { item ->
                val amount = if (item.frequency == Frequency.YEARLY) line.annual else line.monthly
                MoneyEntry.FixedExpenseEntry(item.copy(amounts = item.amounts.from(from, amount)))
            } ?: variable.singleOrNull()?.let { MoneyEntry.VariableBudgetEntry(it.copy(amounts = it.amounts.from(from, line.monthly))) }
            ?: MoneyEntry.PeriodicBudgetEntry(periodic.single().copy(annualAmount = line.annual)),
        ))
    }
}

/** True when no open month of the budget's year comes before [from]: changing its one amount touches only [from] on. */
private fun yearIsClosedBefore(budget: PeriodicBudget, from: YearMonth, data: MoneyData): Boolean {
    val closed = data.closedMonths.map { it.month }.toSet()
    return generateSequence(budget.yearStartMonth) { it.plusMonths(1) }.takeWhile { it < from }.all { it in closed }
}

private fun monthlyOf(item: FixedExpense, month: YearMonth): Money {
    val amount = amountFor(item, month) ?: Money.ZERO
    return if (item.frequency == Frequency.YEARLY) amount / 12 else amount
}

private fun newItem(line: PaamonimLine, category: Category, from: YearMonth, newId: () -> String): MoneyEntry {
    val history = AmountHistory(listOf(AmountChange(from, line.monthly)))
    return when (category.klass) {
        ExpenseClass.FIXED -> MoneyEntry.FixedExpenseEntry(
            FixedExpense(newId(), category.name, history, klass = ExpenseClass.FIXED, categoryId = category.id))
        ExpenseClass.CURRENT -> MoneyEntry.VariableBudgetEntry(
            VariableBudget(newId(), category.name, history, klass = ExpenseClass.CURRENT, categoryId = category.id))
        ExpenseClass.PERIODIC -> MoneyEntry.PeriodicBudgetEntry(
            // The budget year starts at the import month, so no earlier month gets a reserve it never had.
            PeriodicBudget(newId(), category.id, category.name, line.annual, from))
    }
}
