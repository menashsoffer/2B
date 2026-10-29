package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

/** Amounts use minor units; subscription amounts are in their declared currency. */
data class AmountChange(val effectiveFrom: YearMonth, val amount: Money)

data class AmountHistory(
    val changes: List<AmountChange>,
    val overrides: Map<YearMonth, Money> = emptyMap(),
) {
    init {
        require(changes.map { it.effectiveFrom }.distinct().size == changes.size) {
            "Only one default per effective month is allowed"
        }
        require(changes.all { it.amount >= Money.ZERO } && overrides.values.all { it >= Money.ZERO })
    }
}

interface RecurringItem { val id: String; val amounts: AmountHistory }
enum class Frequency { MONTHLY, YEARLY }
enum class Currency { ILS, USD }
enum class AssetKind { ASSET, LIABILITY }

/** Share of actual net income owed to charity. */
enum class TitheRate(val percent: Int) { NONE(0), MAASER(10), CHOMESH(20) }

internal fun requireId(id: String) = require(id.isNotBlank()) { "id must not be blank" }

/** Cards are grouped by id; two cards with the same name and billing day stay separate. */
data class Card(val id: String, val name: String, val billingDay: Int) {
    init { requireId(id); require(billingDay in 1..31) }
}
data class Income(
    override val id: String, val name: String, override val amounts: AmountHistory,
    val titheRate: TitheRate = TitheRate.NONE,
) : RecurringItem {
    init { requireId(id) }
}
/** A YEARLY fixed expense is billed in full in billingMonth. */
data class FixedExpense(
    override val id: String, val name: String, override val amounts: AmountHistory,
    val frequency: Frequency = Frequency.MONTHLY, val card: Card? = null,
    val billingMonth: Month? = null,
    val klass: ExpenseClass = ExpenseClass.FIXED, val categoryId: String? = null,
) : RecurringItem {
    init {
        requireId(id)
        require(frequency != Frequency.YEARLY || billingMonth != null) { "A yearly fixed expense needs a billingMonth" }
    }
}
/** A YEARLY subscription is billed in full in the month of renewalDate. */
data class Subscription(
    override val id: String, val name: String, override val amounts: AmountHistory,
    val currency: Currency = Currency.ILS, val frequency: Frequency = Frequency.MONTHLY,
    val renewalDate: LocalDate, val card: Card? = null,
    val klass: ExpenseClass = ExpenseClass.FIXED, val categoryId: String? = null,
) : RecurringItem {
    init { requireId(id) }
}
data class VariableBudget(
    override val id: String, val name: String, override val amounts: AmountHistory, val card: Card? = null,
    val klass: ExpenseClass = ExpenseClass.CURRENT, val categoryId: String? = null,
) : RecurringItem {
    init { requireId(id) }
}

/** Pass currentMonth explicitly so the default first month is deterministic. */
data class InstallmentPurchase(
    val id: String, val name: String, val total: Money, val currentMonth: YearMonth,
    val numberOfPayments: Int = 1, val firstMonth: YearMonth = currentMonth.plusMonths(1),
    val card: Card? = null,
) {
    init { requireId(id); require(total >= Money.ZERO); require(numberOfPayments > 0) }
}
data class Asset(
    val id: String, val name: String, val type: String, val balance: Money,
    val kind: AssetKind, val liquid: Boolean, val lastUpdated: LocalDate,
) {
    init { requireId(id); require(balance >= Money.ZERO) }
}
data class MonthSnapshot(
    val month: YearMonth, val actualIncomes: Money, val actualExpenses: Money,
    val balances: List<Asset> = emptyList(),
) {
    init { require(actualIncomes >= Money.ZERO); require(actualExpenses >= Money.ZERO) }
}
data class MoneyItems(
    val incomes: List<Income> = emptyList(),
    val fixedExpenses: List<FixedExpense> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val variableBudgets: List<VariableBudget> = emptyList(),
    val installments: List<InstallmentPurchase> = emptyList(),
    val periodicBudgets: List<PeriodicBudget> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val savings: List<Saving> = emptyList(),
) {
    internal fun isEmpty() = incomes.isEmpty() && fixedExpenses.isEmpty() && subscriptions.isEmpty() &&
        variableBudgets.isEmpty() && installments.isEmpty() && periodicBudgets.isEmpty() && debts.isEmpty() && savings.isEmpty()
}
