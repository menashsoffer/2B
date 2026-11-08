package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MoneySamples.shekels
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.AmountChange
import com.menash.myself.feature.money.domain.AmountHistory
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
        assertEquals(YearMonth.of(2026, 1), car.value.yearStartMonth)
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

    @Test fun aPeriodicBudgetGetsTheNewAnnualAmount() {
        val holidays = draft(MoneySamples.filled).line("יהדות / חגים")
        assertEquals(shekels(400), holidays.current)
        assertEquals(shekels(220 * 12), (holidays.entries.single() as MoneyEntry.PeriodicBudgetEntry).value.annualAmount)
    }

    @Test fun subscriptionsTotalIsNotAddedOverListedSubscriptions() {
        val draft = draft(MoneySamples.filled)
        assertEquals(PlanLineStatus.SUBSCRIPTIONS, draft.line("מנויים").status)
        assertTrue("מנויים" !in draft.selectableKeys.map { it.substringAfter(':') })
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
