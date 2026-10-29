package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val bidiControls = Regex("[‎‏‪-‮⁦-⁩﻿]")
private val spaces = Regex("[\\s   ]+")
private val quotes = Regex("[\"'״׳`]")
private val longDigits = Regex("\\d{6,}")
private val hebrewRun = Regex("[֐-׿][֐-׿\\s\"'״׳\\-]*[֐-׿]|[֐-׿]")

/** Drops direction marks, turns every kind of space and line break into one space, trims. */
fun cleanText(text: String): String = text.replace(bidiControls, "").replace(spaces, " ").trim()

/** For matching headers: clean, no quote marks of any kind ("סכום בש\"ח" = "סכום בש״ח" = "סכום בשח"). */
fun headerKey(text: String): String = cleanText(text).replace(quotes, "").lowercase()

/** ID and account numbers can appear inside descriptions ("מזהה 123456789"); runs of 6+ digits are never kept. */
fun maskDigits(text: String): String = text.replace(longDigits, "••••")

/**
 * One Zero stores Hebrew in reverse order behind a left-to-right override (U+202D): "תינועבר תיביר" is
 * "ריבית רבעונית". Only text carrying an override is touched; each Hebrew run is reversed in place.
 */
fun fixReversedHebrew(raw: String): String {
    if (!raw.contains('‭') && !raw.contains('‮')) return cleanText(raw)
    val text = cleanText(raw)
    return hebrewRun.replace(text) { it.value.reversed() }
}

fun Cell?.text(): String? = when (this) {
    is Cell.Text -> cleanText(value).takeIf { it.isNotEmpty() }
    is Cell.Number -> BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    null -> null
}

sealed class AmountCell {
    object Blank : AmountCell()
    object Bad : AmountCell()
    data class Value(val money: Money) : AmountCell()
}

private val currencyNoise = Regex("₪|ש\"ח|ש״ח|שח|NIS|ILS|[$€£,\\s ]", RegexOption.IGNORE_CASE)

/** Exact to the agora: numbers through their decimal text, text with ₪, commas, "100-" and "(100)" forms. */
fun amountOf(cell: Cell?): AmountCell = when (cell) {
    null -> AmountCell.Blank
    is Cell.Number -> AmountCell.Value(toMoney(BigDecimal(cell.value.toString())))
    is Cell.Text -> {
        var t = cleanText(cell.value).replace(currencyNoise, "")
        var negative = false
        if (t.startsWith("(") && t.endsWith(")")) { negative = true; t = t.substring(1, t.length - 1) }
        if (t.endsWith("-")) { negative = !negative; t = t.dropLast(1) }
        if (t.startsWith("-")) { negative = !negative; t = t.drop(1) }
        if (t.isEmpty()) AmountCell.Blank
        else t.toBigDecimalOrNull()?.let { AmountCell.Value(toMoney(if (negative) it.negate() else it)) } ?: AmountCell.Bad
    }
}

private fun toMoney(value: BigDecimal): Money =
    Money(value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact())

private val excelEpoch: LocalDate = LocalDate.of(1899, 12, 30)
private val dateFormats = listOf("d/M/yyyy", "d-M-yyyy", "d.M.yyyy", "d/M/yy", "d-M-yy", "d.M.yy", "yyyy-M-d")
    .map { DateTimeFormatter.ofPattern(it) }

/** Excel serial numbers (time of day dropped) and the text forms the four sources use. Blank or bad → null. */
fun dateOf(cell: Cell?): LocalDate? = when (cell) {
    null -> null
    is Cell.Number -> if (cell.value in 1.0..2_958_465.0) excelEpoch.plusDays(Math.floor(cell.value).toLong()) else null
    is Cell.Text -> {
        val token = cleanText(cell.value).substringBefore(' ').substringBefore('T')
        dateFormats.firstNotNullOfOrNull { format ->
            try {
                LocalDate.parse(token, format).let { if (it.year < 100) it.plusYears(2000) else it }
            } catch (e: DateTimeParseException) {
                null
            }
        }
    }
}

private val installmentText = Regex("תשלום\\s*(\\d+)\\s*מתוך\\s*(\\d+)")

/** "תשלום 2 מתוך 3" anywhere in the given texts. TODO: confirm the exact column once a real sample arrives. */
fun installmentOf(vararg texts: String?): com.menash.myself.feature.money.domain.InstallmentInfo? =
    texts.filterNotNull().firstNotNullOfOrNull { installmentText.find(it) }?.let {
        val number = it.groupValues[1].toInt()
        val of = it.groupValues[2].toInt()
        if (of > 0 && number in 1..of) com.menash.myself.feature.money.domain.InstallmentInfo(number, of) else null
    }

/** Header row index and column per wanted name, found by key containment within the first rows. */
internal fun findColumns(sheet: Sheet, wanted: Map<String, List<String>>, required: Set<String>, scan: Int = 12): Pair<Int, Map<String, Int>>? {
    for (r in 0 until minOf(scan, sheet.rows.size)) {
        val keys = sheet.rows[r].map { cell -> (cell as? Cell.Text)?.let { headerKey(it.value) } }
        val found = wanted.mapNotNull { (name, candidates) ->
            val col = candidates.firstNotNullOfOrNull { c -> keys.indexOfFirst { it != null && it == headerKey(c) }.takeIf { it >= 0 } }
                ?: candidates.firstNotNullOfOrNull { c -> keys.indexOfFirst { it != null && it.contains(headerKey(c)) }.takeIf { it >= 0 } }
            col?.let { name to it }
        }.toMap()
        if (found.keys.containsAll(required)) return r to found
    }
    return null
}
