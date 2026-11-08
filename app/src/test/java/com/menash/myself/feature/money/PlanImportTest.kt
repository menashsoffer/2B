package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MoneySamples.shekels
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.AmountChange
import com.menash.myself.feature.money.domain.AmountHistory
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.domain.MonthSnapshot
import com.menash.myself.feature.money.domain.Income
import com.menash.myself.feature.money.domain.MoneyItems
import com.menash.myself.feature.money.domain.amountFor
import com.menash.myself.feature.money.domain.defaultCategories
import com.menash.myself.feature.money.importer.ImportFixtures
import com.menash.myself.feature.money.importer.PaamonimLine
import com.menash.myself.feature.money.importer.PaamonimPlan
import com.menash.myself.feature.money.importer.PlanGroup
import com.menash.myself.feature.money.importer.readPaamonimPlan
import com.menash.myself.feature.money.importer.readWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

class PlanImportTest {
    private val from = YearMonth.of(2026, 11)
    private val file = requireNotNull(readPaamonimPlan(readWorkbook(ImportFixtures.bytes("paamonim_plan.xls"))))
    private var ids = 0
    private val newId = { "new-${ids++}" }

    private fun draft(data: MoneyData, plan: PaamonimPlan = file) = draftPlanImport(plan, data, from, newId)
    private fun PlanImportDraft.line(name: String) = lines.single { it.name == name }

    @Test fun emptyHouseholdGetsEveryLineAsNew() {
        val draft = draft(MoneySamples.empty)
        assertTrue(draft.lines.all { it.status == PlanLineStatus.NEW })
        assertEquals(draft.lines.map { it.key }.toSet(), draft.selectableKeys)
        assertTrue(draft.seedCategories.isEmpty())
    }

    @Test fun newItemsUseTheRightTypeCategoryAndStartMonth() {
        val draft = draft(MoneySamples.empty)
        val rent = draft.line("שכר דירה").entries.single() as MoneyEntry.FixedExpenseEntry
        assertEquals("fixed-3", rent.value.categoryId)
        assertEquals(listOf(AmountChange(from, shekels(460))), rent.value.amounts.changes)
        val food = draft.line("מזון").entries.single() as MoneyEntry.VariableBudgetEntry
        assertEquals("current-1", food.value.categoryId)
        val car = draft.line("ביטוח רכב + טסט").entries.single() as MoneyEntry.PeriodicBudgetEntry
        assertEquals(shekels(150 * 12), car.value.annualAmount)
        assertEquals(from, car.value.yearStartMonth) // no reserve for months before the import
        val income = draft.line("קצבת ילדים").entries.single() as MoneyEntry.IncomeEntry
        assertEquals(shekels(8_800), amountFor(income.value, from))
    }

    @Test fun fuelLineMatchesTheVehicleEnergyCategory() {
        val fuel = draft(MoneySamples.empty).line("דלק")
        assertEquals("current-3", (fuel.entries.single() as MoneyEntry.VariableBudgetEntry).value.categoryId)
    }

    @Test fun existingItemsChangeFromTheImportMonthOnly() {
        val draft = draft(MoneySamples.filled)
        val rent = draft.line("שכר דירה")
        assertEquals(PlanLineStatus.CHANGED, rent.status)
        assertEquals(shekels(5_200), rent.current)
        val saved = (rent.entries.single() as MoneyEntry.FixedExpenseEntry).value
        assertEquals("fx-rent", saved.id)
        assertEquals(shekels(5_200), amountFor(saved, from.minusMonths(1)))
        assertEquals(shekels(460), amountFor(saved, from))
    }

    @Test fun aYearlyFixedExpenseKeepsItsYearlyAmount() {
        val car = draft(MoneySamples.filled).line("ביטוח רכב + טסט")
        assertEquals(shekels(300), car.current) // 3,600 a year
        val saved = (car.entries.single() as MoneyEntry.FixedExpenseEntry).value
        assertEquals(shekels(1_800), amountFor(saved, from))
    }

    private fun closedThrough(last: YearMonth) = generateSequence(YearMonth.of(2026, 1)) { it.plusMonths(1) }.takeWhile { it <= last }
        .map { ClosedMonth(it, MonthSnapshot(it, Money.ZERO, Money.ZERO), emptyMap(), emptyMap(), emptyMap(), emptyMap(), LocalDate.of(2026, 11, 1)) }
        .toList()

    @Test fun aPeriodicBudgetChangesOnlyWhenItsEarlierMonthsAreClosed() {
        val open = draft(MoneySamples.filled).line("יהדות / חגים")
        assertEquals(PlanLineStatus.PERIODIC_IN_USE, open.status) // January..October still open
        assertEquals(shekels(400), open.current)
        assertTrue(open.entries.isEmpty())
        assertEquals(PlanLineStatus.PERIODIC_IN_USE, draft(MoneySamples.filled.copy(closedMonths = closedThrough(YearMonth.of(2026, 9)))).line("יהדות / חגים").status)
        val closed = draft(MoneySamples.filled.copy(closedMonths = closedThrough(YearMonth.of(2026, 10)))).line("יהדות / חגים")
        assertEquals(PlanLineStatus.CHANGED, closed.status)
        assertEquals(shekels(220 * 12), (closed.entries.single() as MoneyEntry.PeriodicBudgetEntry).value.annualAmount)
    }

    @Test fun aChangeReplacesAThisMonthOnlyOverrideOfTheImportMonth() {
        val rent = MoneySamples.rent.copy(amounts = MoneySamples.rent.amounts.copy(
            overrides = mapOf(from to shekels(6_000), from.plusMonths(2) to shekels(6_100))))
        val data = MoneySamples.filled.copy(items = MoneySamples.filled.items.copy(fixedExpenses = listOf(rent, MoneySamples.carInsurance)))
        val saved = (draft(data).line("שכר דירה").entries.single() as MoneyEntry.FixedExpenseEntry).value
        assertEquals(shekels(460), amountFor(saved, from))
        assertEquals(shekels(6_100), amountFor(saved, from.plusMonths(2))) // a later choice of the household stays
    }

    @Test fun repeatedNamesAreKeptApartAndNotGuessed() {
        val twice = PaamonimPlan(Month.JANUARY, listOf(
            PaamonimLine(PlanGroup.CURRENT, "מזון", Money(100_000), Money(1_200_000)),
            PaamonimLine(PlanGroup.CURRENT, "מזון ", Money(50_000), Money(600_000)),
            PaamonimLine(PlanGroup.CURRENT, "דלק", Money(40_000), Money(480_000)),
            PaamonimLine(PlanGroup.CURRENT, "דלק / חשמל", Money(30_000), Money(360_000)),
            PaamonimLine(PlanGroup.INCOME, "משכורת", Money(900_000), Money(10_800_000)),
            PaamonimLine(PlanGroup.INCOME, "משכורת", Money(800_000), Money(9_600_000)),
            PaamonimLine(PlanGroup.FIXED, "שכר דירה", Money(500_000), Money(6_000_000)),
        ), null)
        val draft = draft(MoneySamples.empty, twice)
        assertEquals(7, draft.lines.map { it.key }.toSet().size)
        assertEquals(6, draft.lines.count { it.status == PlanLineStatus.DUPLICATE })
        assertTrue(draft.lines.filter { it.status == PlanLineStatus.DUPLICATE }.all { it.entries.isEmpty() && !it.selectable })
        assertEquals(1, draft.selectableKeys.size)
        assertEquals(1, draft.toSave(draft.selectableKeys).size)
    }

    @Test fun subscriptionsTotalIsNotAddedOverListedSubscriptions() {
        val draft = draft(MoneySamples.filled)
        assertEquals(PlanLineStatus.SUBSCRIPTIONS, draft.line("מנויים").status)
        assertTrue("מנויים" !in draft.selectableKeys.map { it.substringAfterLast(':') })
        assertEquals(PlanLineStatus.NEW, draft(MoneySamples.empty).line("מנויים").status)
    }

    @Test fun sameAmountWithinAShekelIsLeftAlone() {
        val one = PaamonimPlan(Month.JANUARY, listOf(PaamonimLine(PlanGroup.CURRENT, "מזון", Money(300_050), Money(3_600_600))), null)
        val line = draft(MoneySamples.filled, one).lines.single()
        assertEquals(PlanLineStatus.SAME, line.status)
        assertTrue(draft(MoneySamples.filled, one).selectableKeys.isEmpty())
    }

    @Test fun twoItemsInOneCategoryAreNotGuessed() {
        val second = MoneySamples.food.copy(id = "var-food-2")
        val data = MoneySamples.filled.copy(items = MoneySamples.filled.items.copy(variableBudgets = MoneySamples.filled.items.variableBudgets + second))
        val food = draft(data).line("מזון")
        assertEquals(PlanLineStatus.SPLIT, food.status)
        assertEquals(shekels(6_000), food.current)
        assertTrue(food.entries.isEmpty())
    }

    @Test fun incomesMatchByName() {
        val data = MoneySamples.empty.copy(items = MoneyItems(incomes = listOf(
            Income("inc-kids", "קצבת ילדים", AmountHistory(listOf(AmountChange(YearMonth.of(2026, 1), shekels(8_800))))),
        )))
        val draft = draft(data)
        assertEquals(PlanLineStatus.SAME, draft.line("קצבת ילדים").status)
        assertEquals(PlanLineStatus.NEW, draft.line("בן זוג א").status)
    }

    @Test fun aLineWithItsOwnNameAddsACategory() {
        val custom = PaamonimPlan(Month.JANUARY, listOf(PaamonimLine(PlanGroup.PERIODIC, "חוג שחייה", Money(10_000), Money(120_000))), null)
        val line = draft(MoneySamples.empty, custom).lines.single()
        assertEquals(PlanLineStatus.NEW_CATEGORY, line.status)
        val category = (line.entries[0] as MoneyEntry.CategoryEntry).value
        assertEquals("חוג שחייה", category.name)
        assertEquals(category.id, (line.entries[1] as MoneyEntry.PeriodicBudgetEntry).value.categoryId)
    }

    @Test fun householdWithoutCategoriesSeedsTheDefaultsOnSave() {
        val draft = draft(MoneyData(householdReady = true))
        assertEquals(defaultCategories(), draft.seedCategories)
        val saved = draft.toSave(draft.selectableKeys)
        assertEquals(defaultCategories().map { it.id }, saved.filterIsInstance<MoneyEntry.CategoryEntry>().map { it.value.id })
        assertEquals("fixed-3", draft.line("שכר דירה").entries.filterIsInstance<MoneyEntry.FixedExpenseEntry>().single().value.categoryId)
    }

    @Test fun onlySelectedLinesAreSaved() {
        val draft = draft(MoneySamples.empty)
        val rent = draft.line("שכר דירה")
        assertEquals(rent.entries, draft.toSave(setOf(rent.key)))
        assertEquals(emptyList<MoneyEntry>(), draft.toSave(emptySet()))
    }
}
