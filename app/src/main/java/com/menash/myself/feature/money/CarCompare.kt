package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.Option
import com.menash.myself.feature.money.domain.OptionComparison
import com.menash.myself.feature.money.domain.OptionCost
import com.menash.myself.feature.money.domain.compareOptions
import java.math.BigDecimal

enum class CarOption { CASH, BANK_LOAN, DEALER, LEASING }

fun Option.kind(): CarOption = when (this) {
    is Option.Cash -> CarOption.CASH
    is Option.BankLoan -> CarOption.BANK_LOAN
    is Option.DealerFinancing -> CarOption.DEALER
    is Option.Leasing -> CarOption.LEASING
}

/**
 * The comparison as the screen shows it: [rows] cheapest first, then the options that cannot be compared.
 * [incomplete] are options with some fields typed but not enough to price; [invalid] are options whose numbers
 * contradict each other (a trade-in above the price, a down payment above the price).
 */
data class CarCompareView(
    val horizonMonths: Int,
    val rows: List<OptionComparison>,
    val incomplete: Set<CarOption>,
    val invalid: Set<CarOption>,
) {
    val cheapest: OptionCost? get() = rows.firstOrNull() as? OptionCost
    val isEmpty: Boolean get() = rows.isEmpty() && incomplete.isEmpty() && invalid.isEmpty()
}

private fun money(text: String): Money? = if (text.isBlank()) null else parseAmount(text)
private fun percent(text: String): BigDecimal? =
    text.trim().removeSuffix("%").trim().takeIf { it.isNotEmpty() }?.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO }
private fun months(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it > 0 }
private fun km(text: String): Int? = text.trim().replace(",", "").toIntOrNull()?.takeIf { it >= 0 }

private sealed interface Built {
    data class Ready(val option: Option) : Built
    data class Missing(val kind: CarOption) : Built
    data class Bad(val kind: CarOption) : Built
}

private fun build(kind: CarOption, typed: List<String>, required: List<Any?>, make: () -> Option): Built? = when {
    typed.all { it.isBlank() } -> null
    required.any { it == null } -> Built.Missing(kind)
    else -> try { Built.Ready(make()) } catch (_: IllegalArgumentException) { Built.Bad(kind) }
}

/** Builds each option the draft has numbers for and prices them with the engine's compareOptions. */
fun carCompare(draft: CarCompareDraft, current: CarCurrent): CarCompareView {
    val horizon = draft.horizonYears * 12
    val price = money(draft.price)
    val tradeIn = money(draft.tradeIn) ?: Money.ZERO
    val resale = money(draft.resale) ?: Money.ZERO
    val lost = percent(draft.lostReturn) ?: BigDecimal.ZERO
    val built = listOfNotNull(
        build(CarOption.CASH, listOf(draft.price), listOf(price)) { Option.Cash(price!!, lost, tradeIn, resale) },
        draft.run {
            val rate = percent(loanRate)
            val term = months(loanMonths)
            build(CarOption.BANK_LOAN, listOf(loanDown, loanRate, loanMonths), listOf(price, rate, term)) {
                Option.BankLoan(price!!, money(loanDown) ?: Money.ZERO, rate!!, term!!, tradeIn, resale, lostAnnualReturn = lost)
            }
        },
        draft.run {
            val list = money(dealerList)
            val rate = percent(dealerRate)
            val term = months(dealerMonths)
            build(CarOption.DEALER, listOf(dealerList, dealerRate, dealerMonths, dealerBalloon), listOf(list, rate, term)) {
                // The discount lost by financing: what the same car costs paid in cash.
                val discount = if (price != null && price < list!!) list - price else Money.ZERO
                Option.DealerFinancing(list!!, discount, rate!!, term!!, money(dealerBalloon) ?: Money.ZERO, tradeIn, resale)
            }
        },
        draft.run {
            val monthly = money(leaseMonthly)
            val term = months(leaseMonths)
            build(CarOption.LEASING, listOf(leaseMonthly, leaseUpfront, leaseMonths, leaseKmLimit, leaseExtraKm),
                // A price per extra km means nothing without the km the lease includes.
                listOf(monthly, term, if (leaseExtraKm.isBlank()) 0 else km(leaseKmLimit))) {
                Option.Leasing(
                    monthly!!, money(leaseUpfront) ?: Money.ZERO, term!!, leaseIncluded, km(leaseKmLimit) ?: 0,
                    money(leaseExtraKm) ?: Money.ZERO, tradeIn, km(kmPerYear), lost,
                )
            }
        },
    )
    val options = built.filterIsInstance<Built.Ready>().map { it.option }
    return CarCompareView(
        horizonMonths = horizon,
        rows = if (options.isEmpty()) emptyList() else compareOptions(carProfile(current), options, horizon),
        incomplete = built.filterIsInstance<Built.Missing>().map { it.kind }.toSet(),
        invalid = built.filterIsInstance<Built.Bad>().map { it.kind }.toSet(),
    )
}
