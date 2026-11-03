package com.menash.myself.feature.money.search

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.MerchantMemory
import com.menash.myself.feature.money.domain.defaultCategories
import com.menash.myself.feature.money.importer.ImportIds
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.RowStatus
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.feature.money.importer.StatementRow
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.importer.monthLines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Search over every month's lines uses the same counting and categories as the month screens. */
class LineSearchTest {

    private val categories = defaultCategories()
    private val food = "current-1"
    private val fuel = "current-3"
    private val august = YearMonth.of(2026, 8)
    private val september = YearMonth.of(2026, 9)
    private val october = YearMonth.of(2026, 10)

    private var next = 0
    private fun line(
        description: String,
        shekels: Long,
        date: LocalDate,
        category: String? = null,
        status: RowStatus = RowStatus.COUNTED,
        source: Source = Source.MAX,
    ) = StoredTransaction(
        StatementRow(
            id = "row-${next++}", source = source, sourceId = "card-1", date = date, chargeDate = null,
            description = description, charge = Money(shekels * 100), status = status,
        ),
        category,
    )

    private val stored = listOf(
        line("שופרסל דיל 123", 412, LocalDate.of(2026, 10, 3), food),
        line("שופרסל אונליין", 250, LocalDate.of(2026, 9, 12), food),
        line("פז דלק", 300, LocalDate.of(2026, 10, 5), fuel),
        line("פז דלק", 280, LocalDate.of(2026, 8, 20), fuel),
        line("קפה גרג", 38, LocalDate.of(2026, 10, 7)),
        line("ביט העברה", 120, LocalDate.of(2026, 9, 1)),
        line("שופרסל דיל 123", 99, LocalDate.of(2026, 10, 9), status = RowStatus.PENDING),
    )
    private val memory = ImportMemory(aliases = mapOf(ImportIds.merchantKey("קפה גרג") to "הקפה של יום שישי"))

    private fun find(text: String, scope: LineSearchScope = LineSearchScope()) = searchLines(stored, memory, categories, text, scope)

    @Test fun `empty text finds every line, newest month first`() {
        val result = find("")
        assertEquals(listOf(october, september, august), result.months.map { it.month })
        assertEquals(stored.size, result.count)
    }

    @Test fun `merchant text across months`() {
        val result = find("שופרסל")
        assertEquals(listOf(october, september), result.months.map { it.month })
        assertEquals(3, result.count)
    }

    @Test fun `pending lines are found but never summed`() {
        val result = find("שופרסל")
        assertTrue(result.lines.any { it.pending })
        assertEquals(Money((412 + 250) * 100L), result.spent)
    }

    @Test fun `alias is searchable`() {
        assertEquals(listOf("קפה גרג"), find("יום שישי").lines.map { it.row.description })
    }

    @Test fun `category name is searchable`() {
        assertEquals(2, find("דלק").count)
        assertEquals(setOf("שופרסל דיל 123", "שופרסל אונליין"), find("מזון").lines.map { it.row.description }.toSet())
    }

    @Test fun `amount within one shekel`() {
        assertEquals(listOf("ביט העברה"), find("120").lines.map { it.row.description })
        assertEquals(listOf("פז דלק"), find("299").lines.map { it.row.description })
    }

    @Test fun `range keeps only its months`() {
        val result = find("פז", LineSearchScope(from = september, to = october))
        assertEquals(listOf(october), result.months.map { it.month })
    }

    @Test fun `only unassigned`() {
        val result = find("", LineSearchScope(onlyUnassigned = true))
        assertEquals(setOf("קפה גרג", "ביט העברה"), result.lines.map { it.row.description }.toSet())
    }

    @Test fun `one category`() {
        assertEquals(2, find("", LineSearchScope(categoryId = fuel)).count)
    }

    @Test fun `memory category is searchable like a chosen one`() {
        val learnt = ImportMemory(MerchantMemory(mapOf(ImportIds.merchantKey("ביט העברה") to food)))
        val found = searchLines(stored, learnt, categories, "מזון").lines.map { it.row.description }
        assertTrue("ביט העברה" in found)
    }

    @Test fun `search inside a month matches the month screen lines`() {
        val month = monthLines(stored, october, memory, categories)
        val matching = month.matching("פז 300", memory, categories)
        assertEquals(1, matching.lines.size)
        assertEquals(october, matching.month)
    }

    @Test fun `nothing found gives no months`() {
        assertEquals(0, find("רמי לוי").count)
        assertTrue(find("רמי לוי").months.isEmpty())
    }
}
