package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.planVsActual
import com.menash.myself.feature.money.domain.sumMoney
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.importer.RowStatus
import com.menash.myself.feature.money.importer.RowKind
import com.menash.myself.feature.money.importer.classify
import com.menash.myself.feature.money.importer.actualIncome
import com.menash.myself.feature.money.importer.toTransactions
import java.math.BigDecimal
import java.time.YearMonth

private fun MoneyEntry.categoryId(): String? = when (this) {
    is MoneyEntry.FixedExpenseEntry -> value.categoryId
    is MoneyEntry.SubscriptionEntry -> value.categoryId
    is MoneyEntry.VariableBudgetEntry -> value.categoryId
    is MoneyEntry.PeriodicBudgetEntry -> value.categoryId
    else -> null
}

private val classOfRow = mapOf(
    MonthRowKind.FIXED to ExpenseClass.FIXED,
    MonthRowKind.CURRENT to ExpenseClass.CURRENT,
    MonthRowKind.PERIODIC to ExpenseClass.PERIODIC,
)

/**
 * Fills "בפועל" from imported lines through the engine's planVsActual. A month with no counted
 * imported line is returned unchanged, so nothing shows an invented zero. A row's actual is the sum of its
 * class's categories; an item shows its own actual only when no other item shares its category, since one
 * category's spending cannot be split between items. Spending without a known category is [MonthView.Planned.unassigned].
 */
internal fun withActuals(
    view: MonthView.Planned,
    data: MoneyData,
    month: YearMonth,
    rate: BigDecimal?,
    stored: List<StoredTransaction>,
    memory: ImportMemory,
): MonthView.Planned {
    val lines = stored.count { it.row.billingMonth == month }
    val transactions = toTransactions(stored, memory).filter { it.billingMonth == month }
    val income = actualIncome(stored, month, memory)
    val incomeKnown = stored.any { it.row.billingMonth == month && it.row.status == RowStatus.COUNTED &&
        (it.row.charge?.agorot ?: 0L) < 0L && classify(it.row, memory) in setOf(RowKind.INCOME, RowKind.TRANSFER) }
    if (transactions.isEmpty() && income.isZero()) return view.copy(
        rows = view.rows.map { if (it.kind == MonthRowKind.INCOME && lines > 0) it.copy(actualKnown = incomeKnown) else it },
        importedLines = lines,
    )
    val result = planVsActual(month, data.items, transactions, rate)
    val classOf = data.categories.associate { it.id to it.klass }
    val byCategory = result.categories.associate { it.categoryId to it.actual }
    val byPurchase = result.installments.associate { it.purchaseId to it.actual }
    val shared = view.rows.flatMap { it.items }.mapNotNull { it.entry.categoryId() }
        .groupingBy { it }.eachCount().filterValues { it > 1 }.keys

    val rows = view.rows.map { row ->
        val klass = classOfRow[row.kind]
        when {
            row.kind == MonthRowKind.INCOME -> row.copy(actual = income.takeIf { incomeKnown }, actualKnown = incomeKnown)
            klass != null -> row.copy(
                actual = sumMoney(result.categories.filter { it.categoryId != null && classOf[it.categoryId] == klass }.map { it.actual }),
                items = row.items.map { item ->
                    val category = item.entry.categoryId()
                    if (category != null && category !in shared) item.copy(actual = byCategory[category] ?: Money.ZERO) else item
                },
            )
            row.kind == MonthRowKind.INSTALLMENTS -> row.copy(
                actual = sumMoney(byPurchase.values),
                items = row.items.map { it.copy(actual = byPurchase[it.entry.id] ?: Money.ZERO) },
            )
            else -> row
        }
    }
    val unassigned = sumMoney(result.categories.filter { it.categoryId == null || it.categoryId !in classOf }.map { it.actual })
    return view.copy(rows = rows, unassigned = unassigned.takeUnless { it.isZero() }, importedLines = lines, categoryActuals = result.categories)
}
