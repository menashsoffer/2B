package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.domain.CarProfile
import com.menash.myself.feature.money.domain.Category
import com.menash.myself.feature.money.domain.Energy
import com.menash.myself.feature.money.domain.Included
import com.menash.myself.feature.money.domain.carActualYearly
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.importer.toTransactions
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/** Paamonim ids: ביטוח רכב + טסט, תיקוני רכב, דלק, חניה - כביש 6. */
private const val CAR_INSURANCE_AND_TEST = "periodic-8"
private const val CAR_REPAIRS = "periodic-9"
val DEFAULT_CAR_CATEGORIES = setOf(CAR_INSURANCE_AND_TEST, CAR_REPAIRS, "current-3", "current-12")

/** The car settings document: which categories are "the car", and the last new-car comparison as typed. */
data class CarPlan(
    val categoryIds: Set<String> = DEFAULT_CAR_CATEGORIES,
    val compare: CarCompareDraft = CarCompareDraft(),
) {
    companion object {
        const val ID = "settings"
    }
}

/** The new-car comparison as typed; amounts in shekels, rates in percent, months as whole numbers. */
data class CarCompareDraft(
    val horizonYears: Int = 5,
    val kmPerYear: String = "",
    val price: String = "",
    val tradeIn: String = "",
    val resale: String = "",
    val lostReturn: String = "",
    val loanDown: String = "",
    val loanRate: String = "",
    val loanMonths: String = "",
    val dealerList: String = "",
    val dealerRate: String = "",
    val dealerMonths: String = "",
    val dealerBalloon: String = "",
    val leaseMonthly: String = "",
    val leaseUpfront: String = "",
    val leaseMonths: String = "",
    val leaseKmLimit: String = "",
    val leaseExtraKm: String = "",
    val leaseIncluded: Set<Included> = Included.entries.toSet(),
)

data class CarCategoryCost(val category: Category, val yearly: Money)

/**
 * The current car from the imported lines of the 12 billing months ending at [endMonth]. [coveredMonths] counts
 * the months in that window with any imported line, so a partial year is shown as partial, never stretched.
 */
data class CarCurrent(
    val endMonth: YearMonth,
    val yearly: Money?,
    val monthly: Money?,
    val byCategory: List<CarCategoryCost>,
    val coveredMonths: Int,
)

/** The last full month: the current one is still being billed. */
fun carEndMonth(today: LocalDate): YearMonth = YearMonth.from(today).minusMonths(1)

fun carCurrent(data: MoneyData, plan: CarPlan, stored: List<StoredTransaction>, memory: ImportMemory, today: LocalDate): CarCurrent {
    val end = carEndMonth(today)
    val rows = toTransactions(stored, memory)
    val window = end.minusMonths(11)..end
    val yearly = carActualYearly(rows, plan.categoryIds, end)
    val byCategory = data.categories.filter { it.id in plan.categoryIds }.mapNotNull { category ->
        carActualYearly(rows, setOf(category.id), end)?.let { CarCategoryCost(category, it) }
    }
    val covered = stored.map { it.row.billingMonth }.filter { it in window }.toSet().size
    val monthly = yearly?.let { Money(it.agorot.toBigDecimal().divide(BigDecimal(12), 0, RoundingMode.HALF_UP).longValueExact()) }
    return CarCurrent(end, yearly, monthly, byCategory, covered)
}

/**
 * The car the comparison runs on: today's real costs, assumed for the new car too. Insurance with the test and
 * repairs stay apart so a lease that includes them can take them out; the rest (fuel, parking) is one known amount.
 */
fun carProfile(current: CarCurrent): CarProfile {
    fun of(id: String) = current.byCategory.firstOrNull { it.category.id == id }?.yearly ?: Money.ZERO
    val insurance = of(CAR_INSURANCE_AND_TEST)
    val repairs = of(CAR_REPAIRS)
    val rest = (current.yearly ?: Money.ZERO) - insurance - repairs
    val known = Energy.Known(if (rest.isNegative()) Money.ZERO else rest)
    return CarProfile("current", "", known, insuranceYearly = insurance, maintenanceYearly = repairs)
}
