package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.ImportCheck
import com.menash.myself.feature.money.domain.PaymentMethod
import com.menash.myself.feature.money.domain.Transaction
import com.menash.myself.feature.money.domain.importCheck

/**
 * Reads one exported file. The source is recognised from its header row, never from the file name;
 * the first sheet that matches a source is used. Throws [ImportException] when the file is refused.
 */
fun parseStatement(bytes: ByteArray): ParsedStatement {
    val workbook = readWorkbook(bytes)
    for (sheet in workbook.sheets) {
        when {
            MaxParser.matches(sheet) -> return MaxParser.parse(sheet)
            CalParser.matches(sheet) -> return CalParser.parse(sheet)
            HapoalimParser.matches(sheet) -> return HapoalimParser.parse(sheet)
            OneZeroParser.matches(sheet) -> return OneZeroParser.parse(sheet)
        }
    }
    throw ImportException(FileProblem.UNKNOWN_FORMAT, "No sheet matches a known statement")
}

/** The file's own total against our rows; null when the file has none (bank files: see BalanceBreak). */
fun totalCheck(statement: ParsedStatement): ImportCheck? {
    val total = statement.statementTotal ?: return null
    val rows = statement.checkedRows.map {
        Transaction(it.id, it.date, it.billingMonth, it.description, it.charge ?: Money.ZERO, PaymentMethod.CREDIT_CARD, it.sourceId)
    }
    return importCheck(rows, total)
}
