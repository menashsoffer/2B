package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.domain.Currency
import com.menash.myself.feature.money.domain.Frequency
import com.menash.myself.feature.money.domain.MoneyItems
import com.menash.myself.feature.money.domain.MonthPlan
import com.menash.myself.feature.money.domain.Subscription
import com.menash.myself.feature.money.domain.Transaction
import com.menash.myself.feature.money.domain.amountFor
import com.menash.myself.feature.money.domain.defaultCategories
import com.menash.myself.feature.money.domain.monthPlan
import com.menash.myself.feature.money.domain.sumMoney
import com.menash.myself.feature.money.importer.ImportIds
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.importer.toTransactions
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/** The Paamonim "מנויים" category; its children are the subscription categories. */
val SUBSCRIPTIONS_CATEGORY: String = defaultCategories().first { it.id == "sub-1" }.parentId!!

/** What a card or account last charged for a subscription, from imported lines. */
data class LastCharge(val month: YearMonth, val amount: Money)

/**
 * One stored subscription. [monthly] is its share of a month in shekels (a yearly one divided by 12); null for a
 * USD subscription while no rate is stored. [renewsOn] is the next yearly renewal. [lastCharge] comes from imported
 * lines in its category, only when no other subscription shares that category. [planned] is what the plan expects
 * that charge to be, so a USD charge shows the card's conversion on top of the rate.
 */
data class SubscriptionRow(
    val subscription: Subscription,
    val monthly: Money?,
    val renewsOn: LocalDate?,
    val lastCharge: LastCharge?,
    val planned: Money?,
)

/** A merchant charged in a subscription category that no stored subscription covers. */
data class UnlistedCharge(val merchantKey: String, val name: String, val categoryId: String, val last: LastCharge, val months: Int)

data class SubscriptionsView(
    val rows: List<SubscriptionRow>,
    /** Null while a USD subscription has no rate. */
    val monthlyTotal: Money?,
    val usdRateMissing: Boolean,
    val unlisted: List<UnlistedCharge>,
)

private fun nextRenewal(sub: Subscription, today: LocalDate): LocalDate {
    var date = sub.renewalDate
    while (date < today) date = date.plusYears(1)
    while (date.minusYears(1) >= today) date = date.minusYears(1)
    return date
}

private fun lastCharge(lines: List<Transaction>): LastCharge? {
    val month = lines.maxOfOrNull { it.billingMonth } ?: return null
    return LastCharge(month, sumMoney(lines.filter { it.billingMonth == month }.map { it.amount }))
}

/**
 * The subscriptions screen: the stored subscriptions with what they really charged, and charges in a subscription
 * category over the last 12 months that are not listed yet, so they can be added from what was spent.
 */
fun subscriptionsView(
    data: MoneyData,
    today: LocalDate,
    usdToIls: BigDecimal?,
    stored: List<StoredTransaction>,
    memory: ImportMemory,
): SubscriptionsView {
    val month = YearMonth.from(today)
    val rate = usdToIls?.takeIf { it > BigDecimal.ZERO }
    val window = month.minusMonths(11)..month
    val counted = toTransactions(stored, memory).filter { it.billingMonth in window }
    val active = data.items.subscriptions.filter { amountFor(it, month) != null }
    val shared = active.mapNotNull { it.categoryId }.groupingBy { it }.eachCount().filterValues { it > 1 }.keys

    val rows = active.sortedBy { it.name }.map { sub ->
        val priced = sub.currency == Currency.ILS || rate != null
        val plan = if (priced) monthPlan(month, MoneyItems(subscriptions = listOf(sub)), rate) as? MonthPlan.Planned else null
        val category = sub.categoryId?.takeIf { it !in shared }
        val charge = category?.let { id -> lastCharge(counted.filter { it.categoryId == id }) }
        // A yearly subscription charges its whole amount at renewal, a monthly one its amount: either way the charge
        // is the stored amount, in shekels at today's rate. The card's conversion fee shows as the difference.
        val planned = charge?.takeIf { priced }?.let { amountFor(sub, it.month) }?.let { amount ->
            if (sub.currency == Currency.USD) Money(amount.agorot.toBigDecimal().multiply(rate).setScale(0, RoundingMode.HALF_UP).longValueExact())
            else amount
        }
        SubscriptionRow(sub, plan?.subscriptions, sub.renewalDate.takeIf { sub.frequency == Frequency.YEARLY }?.let { nextRenewal(sub, today) }, charge, planned)
    }

    val subCategories = data.categories.filter { it.id == SUBSCRIPTIONS_CATEGORY || it.parentId == SUBSCRIPTIONS_CATEGORY }.map { it.id }.toSet()
    val covered = active.mapNotNull { it.categoryId }.toSet()
    val unlisted = counted.filter { it.categoryId in subCategories && it.categoryId !in covered }
        .groupBy { ImportIds.merchantKey(it.merchant) }
        .mapNotNull { (key, lines) ->
            val last = lastCharge(lines) ?: return@mapNotNull null
            val latest = lines.maxBy { it.date }
            UnlistedCharge(key, latest.merchant, latest.categoryId!!, last, lines.map { it.billingMonth }.distinct().size)
        }
        .sortedWith(compareByDescending<UnlistedCharge> { it.last.month }.thenBy { it.name })

    val missing = rows.any { it.monthly == null }
    return SubscriptionsView(rows, if (missing) null else sumMoney(rows.mapNotNull { it.monthly }), missing, unlisted)
}
