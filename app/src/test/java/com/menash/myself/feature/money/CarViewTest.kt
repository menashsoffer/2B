package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyCollection
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyDocuments
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.Included
import com.menash.myself.feature.money.domain.NotComparable
import com.menash.myself.feature.money.domain.NotComparableReason
import com.menash.myself.feature.money.domain.Option
import com.menash.myself.feature.money.domain.OptionCost
import com.menash.myself.feature.money.domain.defaultCategories
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.RowStatus
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.feature.money.importer.StatementRow
import com.menash.myself.feature.money.importer.StoredTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** "כמה עולה לנו הרכב": today's car from the imported lines, and the new-car options priced by the engine. */
class CarViewTest {

    private val today = LocalDate.of(2026, 10, 30)
    private val data = MoneyData(householdReady = true, categories = defaultCategories())

    private var n = 0
    private fun line(date: LocalDate, shekels: Long, category: String?) = StoredTransaction(
        StatementRow(
            id = "row-${n++}", source = Source.MAX, sourceId = "1234", date = date, chargeDate = date,
            description = "line $n", charge = Money(shekels * 100), status = RowStatus.COUNTED,
        ),
        categoryId = category,
    )

    private val year = listOf(
        line(LocalDate.of(2026, 3, 10), 3_600, "periodic-8"), // insurance + test
        line(LocalDate.of(2026, 6, 10), 1_200, "periodic-9"), // repairs
        line(LocalDate.of(2026, 9, 10), 600, "current-3"), // fuel
        line(LocalDate.of(2026, 9, 12), 1_000, "current-1"), // food: not the car
        line(LocalDate.of(2026, 10, 5), 400, "current-3"), // October is still being billed
        line(LocalDate.of(2025, 9, 10), 500, "current-3"), // before the 12-month window
    )

    @Test
    fun current_lastTwelveFullMonths_byCategory_andCoverage() {
        val c = carCurrent(data, CarPlan(), year, ImportMemory(), today)
        assertEquals(YearMonth.of(2026, 9), c.endMonth)
        assertEquals(Money(540_000), c.yearly)
        assertEquals(Money(45_000), c.monthly)
        assertEquals(listOf("periodic-8", "periodic-9", "current-3"), c.byCategory.map { it.category.id })
        assertEquals(3, c.coveredMonths)
    }

    @Test
    fun current_noCarLines_isNullNotZero() {
        val c = carCurrent(data, CarPlan(), listOf(line(LocalDate.of(2026, 9, 1), 100, "current-1")), ImportMemory(), today)
        assertNull(c.yearly)
        assertNull(c.monthly)
        assertTrue(c.byCategory.isEmpty())
    }

    @Test
    fun profile_keepsInsuranceAndRepairsApartForTheLease() {
        val p = carProfile(carCurrent(data, CarPlan(), year, ImportMemory(), today))
        assertEquals(Money(360_000), p.insuranceYearly)
        assertEquals(Money(120_000), p.maintenanceYearly)
        assertEquals(com.menash.myself.feature.money.domain.Energy.Known(Money(60_000)), p.energy)
    }

    private val current get() = carCurrent(data, CarPlan(), year, ImportMemory(), today)

    @Test
    fun compare_buildsOnlyTypedOptions_andMarksHalfFilledOnes() {
        val view = carCompare(CarCompareDraft(price = "150000", loanRate = "5.5", leaseMonthly = "2500"), current)
        assertEquals(listOf(CarOption.CASH), view.rows.map { it.option.kind() })
        assertEquals(setOf(CarOption.BANK_LOAN, CarOption.LEASING), view.incomplete)
        assertTrue(view.invalid.isEmpty())
        assertEquals(60, view.horizonMonths)
    }

    @Test
    fun compare_dealerZeroPercentPaysForTheLostCashDiscount() {
        val draft = CarCompareDraft(price = "140000", dealerList = "150000", dealerRate = "0", dealerMonths = "60")
        val view = carCompare(draft, current)
        val cash = view.rows.single { it.option.kind() == CarOption.CASH } as OptionCost
        val dealer = view.rows.single { it.option.kind() == CarOption.DEALER } as OptionCost
        assertEquals(Money(1_000_000), (dealer.option as Option.DealerFinancing).cashDiscount)
        assertEquals(cash, view.cheapest)
        assertEquals(1_000_000L, dealer.netTotal.agorot - cash.netTotal.agorot)
    }

    @Test
    fun compare_tradeInAbovePrice_isInvalidNotACrash() {
        val view = carCompare(CarCompareDraft(price = "50000", tradeIn = "60000"), current)
        assertEquals(setOf(CarOption.CASH), view.invalid)
        assertTrue(view.rows.isEmpty())
    }

    @Test
    fun compare_leaseWithExtraKmPrice_needsKmLimitAndYearlyKm() {
        val base = CarCompareDraft(leaseMonthly = "2500", leaseMonths = "60", leaseExtraKm = "0.5")
        assertEquals(setOf(CarOption.LEASING), carCompare(base, current).incomplete)
        val noKm = carCompare(base.copy(leaseKmLimit = "15000"), current).rows.single()
        assertEquals(NotComparableReason.MISSING_ANNUAL_DISTANCE, (noKm as NotComparable).reason)
        val priced = carCompare(base.copy(leaseKmLimit = "15000", kmPerYear = "20000"), current).rows.single() as OptionCost
        assertTrue(priced.running > Money.ZERO)
    }

    @Test
    fun compare_horizonFollowsTheChosenYears() {
        assertEquals(84, carCompare(CarCompareDraft(horizonYears = 7, price = "1"), current).horizonMonths)
    }

    @Test
    fun plan_roundTripsThroughFirestore_withDefaultsForMissingFields() {
        val plan = CarPlan(
            setOf("current-3", "periodic-8"),
            CarCompareDraft(horizonYears = 7, price = "150000", leaseIncluded = setOf(Included.INSURANCE), leaseExtraKm = "0.4"),
        )
        val entry = MoneyEntry.CarEntry(plan)
        assertEquals(entry, MoneyDocuments.decode(MoneyCollection.CAR, CarPlan.ID, MoneyDocuments.encode(entry)))
        assertEquals(MoneyEntry.CarEntry(CarPlan()), MoneyDocuments.decode(MoneyCollection.CAR, CarPlan.ID, emptyMap()))
    }
}
