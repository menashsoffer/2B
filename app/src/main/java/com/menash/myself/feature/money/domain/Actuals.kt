package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class PaymentMethod { CASH, CHECK, CREDIT_CARD, STANDING_ORDER, VOUCHER }

/** Payment [number] of [of] of an installment purchase, as printed on the statement. */
data class InstallmentInfo(val number: Int, val of: Int) {
    init { require(of > 0 && number in 1..of) }
}

/**
 * One imported (or manually added cash) transaction. amount is what this statement charged, in ILS;
 * refunds are negative. billingMonth is the statement month ("חודש החיוב"); sourceId is the card id or bank account.
 */
data class Transaction(
    val id: String, val date: LocalDate, val billingMonth: YearMonth, val merchant: String, val amount: Money,
    val paymentMethod: PaymentMethod, val sourceId: String, val categoryId: String? = null,
    val originalAmount: Money? = null, val originalCurrency: String = "ILS", val installment: InstallmentInfo? = null,
) {
    init { requireId(id) }
}

/** categoryId null is the "uncategorised" row. difference = planned − actual (positive = under budget). */
data class CategoryActual(val categoryId: String?, val planned: Money, val actual: Money) {
    val difference: Money get() = Money(Math.subtractExact(planned.agorot, actual.agorot))
}
data class InstallmentActual(val purchaseId: String, val planned: Money, val transactionId: String, val actual: Money)
data class PlanVsActual(val month: YearMonth, val categories: List<CategoryActual>, val installments: List<InstallmentActual>)

private fun scheduledPayment(purchase: InstallmentPurchase, month: YearMonth): Money? {
    val index = ChronoUnit.MONTHS.between(purchase.firstMonth, month).toInt()
    return installmentSchedule(purchase).getOrNull(index)?.amount
}

/**
 * Planned per category comes from items' categoryId (monthly equivalents; periodic budgets reserve annual / 12).
 * A transaction with installment info that matches an InstallmentPurchase (same card id as sourceId, a payment
 * scheduled this month, amount within 1 agora) counts once, against that purchase, never also against a category.
 * Each purchase matches at most one transaction.
 */
fun planVsActual(month: YearMonth, items: MoneyItems, transactions: List<Transaction>, usdToIls: java.math.BigDecimal?): PlanVsActual {
    val active = items.forMonth(month)
    val planned = linkedMapOf<String?, Money>()
    fun plan(categoryId: String?, amount: Money) { planned[categoryId] = sumMoney(listOf(planned[categoryId] ?: Money.ZERO, amount)) }
    fun monthlyOf(single: MoneyItems): Money = when (val p = monthPlan(month, single, usdToIls)) {
        is MonthPlan.Planned -> p.plannedExpenses
        MonthPlan.Empty -> Money.ZERO
    }
    active.fixedExpenses.forEach { plan(it.categoryId, monthlyOf(MoneyItems(fixedExpenses = listOf(it)))) }
    active.subscriptions.forEach { plan(it.categoryId, monthlyOf(MoneyItems(subscriptions = listOf(it)))) }
    active.variableBudgets.forEach { plan(it.categoryId, monthlyOf(MoneyItems(variableBudgets = listOf(it)))) }
    active.periodicBudgets.forEach { plan(it.categoryId, monthlyReserve(it)) }

    val unmatched = active.installments.toMutableList()
    val matches = mutableListOf<InstallmentActual>()
    val actual = linkedMapOf<String?, Money>()
    for (tx in transactions.filter { it.billingMonth == month }) {
        val match = if (tx.installment == null) null else unmatched.firstOrNull { p ->
            val payment = scheduledPayment(p, month)
            p.card?.id == tx.sourceId && payment != null && Math.abs(payment.agorot - tx.amount.agorot) <= 1
        }
        if (match != null) {
            unmatched.remove(match)
            matches += InstallmentActual(match.id, scheduledPayment(match, month)!!, tx.id, tx.amount)
        } else {
            actual[tx.categoryId] = sumMoney(listOf(actual[tx.categoryId] ?: Money.ZERO, tx.amount))
        }
    }
    val ids = (planned.keys + actual.keys).distinct()
    return PlanVsActual(month, ids.map { CategoryActual(it, planned[it] ?: Money.ZERO, actual[it] ?: Money.ZERO) }, matches)
}

/** State of a periodic (sinking fund) budget in [month] of its budget year. */
data class PeriodicStatus(val accruedToDate: Money, val spentToDate: Money, val remainingThisYear: Money) {
    /** Positive = spent ahead of what has accrued so far. */
    val advance: Money get() = Money(Math.subtractExact(spentToDate.agorot, accruedToDate.agorot))
}

/**
 * month must be within the 12-month budget year that starts at yearStartMonth. spentToDate counts transactions
 * of the budget's category billed from the year start through [month].
 */
fun periodicStatus(budget: PeriodicBudget, month: YearMonth, transactions: List<Transaction>): PeriodicStatus {
    val elapsed = ChronoUnit.MONTHS.between(budget.yearStartMonth, month) + 1
    require(elapsed in 1..12) { "$month is outside the budget year starting ${budget.yearStartMonth}" }
    val accrued = Money(Math.multiplyExact(budget.annualAmount.agorot, elapsed)) / 12
    val spent = sumMoney(transactions.filter {
        it.categoryId == budget.categoryId && it.billingMonth >= budget.yearStartMonth && it.billingMonth <= month
    }.map { it.amount })
    return PeriodicStatus(accrued, spent, Money(Math.subtractExact(budget.annualAmount.agorot, spent.agorot)))
}

sealed class ImportCheck {
    object Ok : ImportCheck()
    /** difference = Σ rows − statement total. */
    data class Mismatch(val difference: Money) : ImportCheck()
}

fun importCheck(rows: List<Transaction>, statementTotal: Money): ImportCheck {
    val difference = Math.subtractExact(sumMoney(rows.map { it.amount }).agorot, statementTotal.agorot)
    return if (difference == 0L) ImportCheck.Ok else ImportCheck.Mismatch(Money(difference))
}

/** Normalised merchant name to category id. */
data class MerchantMemory(val categories: Map<String, String> = emptyMap())

private val trailingNoise = Regex("""(\s+(סניף\s*)?[#*]?\d+)+$""")

/**
 * Trims, collapses spaces, lower-cases Latin letters and drops trailing branch or card numbers
 * ("שופרסל דיל 123", "סניף 12", "*1234"). Hebrew text is kept as is; different words never merge.
 */
fun normalizeMerchant(name: String): String =
    name.trim().replace(Regex("""\s+"""), " ").lowercase().replace(trailingNoise, "").trim()

fun suggestCategory(merchant: String, memory: MerchantMemory): String? = memory.categories[normalizeMerchant(merchant)]

fun learn(memory: MerchantMemory, merchant: String, categoryId: String): MerchantMemory {
    val key = normalizeMerchant(merchant)
    require(key.isNotEmpty()) { "Merchant name is empty" }
    return MerchantMemory(memory.categories + (key to categoryId))
}
