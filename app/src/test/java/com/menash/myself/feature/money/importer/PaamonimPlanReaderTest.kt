package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Month

class PaamonimPlanReaderTest {
    private val plan = requireNotNull(readPaamonimPlan(readWorkbook(ImportFixtures.bytes("paamonim_plan.xls"))))
    private fun line(name: String) = plan.lines.single { it.name == name }

    @Test fun readsTheLatestBudgetedMonth() {
        assertEquals(Month.JANUARY, plan.month)
    }

    @Test fun groupsFollowTheSheet() {
        assertEquals(PlanGroup.FIXED, line("שכר דירה").group)
        assertEquals(PlanGroup.PERIODIC, line("ביטוח רכב + טסט").group)
        assertEquals(PlanGroup.CURRENT, line("מזון").group)
        assertEquals(PlanGroup.INCOME, line("קצבת ילדים").group)
    }

    @Test fun amountsAreMonthlyWithAnAnnualFigure() {
        assertEquals(Money(46_000), line("שכר דירה").monthly)
        assertEquals(Money(150 * 12 * 100L), line("ביטוח רכב + טסט").annual)
    }

    @Test fun skipsUnusedAndUnbudgetedLines() {
        assertTrue(plan.lines.none { it.name == "משכנתא" }) // named, no budget
        assertTrue(plan.lines.none { it.name == "0" || it.name.isBlank() }) // unused rows carry the number 0
        assertTrue(plan.lines.none { it.name.startsWith("הכנסה נוספת") }) // custom income lines left empty
        assertTrue(plan.lines.all { it.monthly.isPositive() })
    }

    @Test fun totalsRowsAreNotLines() {
        assertTrue(plan.lines.none { it.name.contains("סה") })
        assertEquals(PlanGroup.entries.toSet(), plan.lines.map { it.group }.toSet())
    }

    @Test fun debtRepaymentIsReadSeparately() {
        assertEquals(Money(28_000), plan.debtRepayment)
    }

    @Test fun statementFilesAreNotPlans() {
        listOf("hapoalim_oct.xlsx", "max_nov.xlsx", "onezero_ils.xls", "cal_nov.xlsm").forEach { name ->
            assertNull(name, readPaamonimPlan(readWorkbook(ImportFixtures.bytes(name))))
        }
    }

    @Test fun aSheetWithoutBudgetsReadsAsAnEmptyPlan() {
        val months = listOf("ינואר", "פברואר", "מרץ", "אפריל", "מאי", "יוני", "יולי", "אוגוסט", "ספטמבר", "אוקטובר", "נובמבר", "דצמבר")
        val header = listOf<Cell?>(Cell.Text("סעיף תקציב"), null) + months.map { Cell.Text(it) }
        val row = listOf<Cell?>(Cell.Text("הוצאות שוטפות"), Cell.Text("מזון")) + months.map { Cell.Number(0.0) }
        val plan = readPaamonimPlan(Workbook(listOf(Sheet("רשימת תקציבים", listOf(header, row)))))
        assertEquals(emptyList<PaamonimLine>(), plan?.lines)
    }
}
