package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.MerchantMemory
import com.menash.myself.feature.money.domain.PaymentMethod
import com.menash.myself.feature.money.domain.Transaction
import com.menash.myself.feature.money.domain.suggestCategory
import java.time.YearMonth

enum class RowKind {
    EXPENSE, INCOME,
    /** A bank line paying a card's whole bill; the card file carries the purchases. */
    CARD_TOTAL,
    /** bit / transfer: counted until the user marks the name as one of the household's own accounts. */
    TRANSFER, OWN_TRANSFER,
    /** One Zero securities and FX conversion: outside the budget. */
    TRADING,
    /** Money arriving in One Zero by transfer: a portfolio deposit, outside the budget. */
    PORTFOLIO_DEPOSIT,
    /** Paamonim counts cash when it leaves the bank, in "unassigned cash" until split. */
    CASH_WITHDRAWAL,
    /** Offered once as a fixed expense; counted as an expense meanwhile. */
    STANDING_ORDER,
}

enum class Issuer { MAX, CAL, ISRACARD, AMEX, DINERS }

/** What the household taught the app: merchant → category, and names that are its own accounts. */
data class ImportMemory(
    val merchants: MerchantMemory = MerchantMemory(),
    val ownTransfers: Set<String> = emptySet(),
    /** The household's own name for a merchant, by merchant key. Matching always uses the bank's text. */
    val aliases: Map<String, String> = emptyMap(),
    /** Generic merchants (ביט, העברה): never learnt, every line gets its own category. */
    val askEachTime: Set<String> = emptySet(),
) {
    /** The name to show for a bank line: the alias when the household gave one, else the bank's text. */
    fun displayName(description: String): String = aliases[ImportIds.merchantKey(description)] ?: description

    fun asksEachTime(description: String): Boolean = ImportIds.merchantKey(description) in askEachTime
}

private fun has(text: String, vararg words: String) = words.any { text.contains(headerKey(it)) }

/** Whole words only, for short ones that hide inside others ("ביט" in "ביטוח"). */
private fun hasWord(text: String, vararg words: String) =
    words.any { Regex("(^|[\\s\\-])${Regex.escape(headerKey(it))}($|[\\s\\-])").containsMatchIn(text) }

/** The card company a bank line pays, or null when it is not a card bill. */
fun issuerOf(description: String): Issuer? {
    val t = headerKey(description)
    return when {
        has(t, "מקס איט", "max it", "לאומי קארד") || t.startsWith("מקס") -> Issuer.MAX
        Regex("(^|\\s)כאל(\\s|$)").containsMatchIn(t) || has(t, "כרטיסי אשראי לישראל", "cal-online") -> Issuer.CAL
        has(t, "ישראכרט", "isracard") -> Issuer.ISRACARD
        has(t, "אמריקן אקספרס", "american express", "אמקס") -> Issuer.AMEX
        has(t, "דיינרס", "diners") -> Issuer.DINERS
        else -> null
    }
}

fun issuerOf(source: Source): Issuer? = when (source) {
    Source.MAX -> Issuer.MAX
    Source.CAL -> Issuer.CAL
    else -> null
}

fun classify(row: StatementRow, memory: ImportMemory = ImportMemory()): RowKind {
    val text = headerKey(row.description + " " + row.operation.orEmpty())
    val outflow = (row.charge ?: row.originalAmount ?: Money.ZERO).agorot > 0
    val own = ImportIds.merchantKey(row.description) in memory.ownTransfers
    return when (row.source) {
        Source.ONE_ZERO -> {
            val op = headerKey(row.operation.orEmpty())
            when {
                has(op, "ניירות ערך", "המרת מט\"ח") -> RowKind.TRADING
                has(op, "העברות") && !outflow -> RowKind.PORTFOLIO_DEPOSIT
                has(op, "העברות") -> if (own) RowKind.OWN_TRANSFER else RowKind.TRANSFER
                outflow -> RowKind.EXPENSE
                else -> RowKind.INCOME
            }
        }
        Source.HAPOALIM -> when {
            outflow && issuerOf(row.description) != null -> RowKind.CARD_TOTAL
            has(text, "משיכה מבנקט", "משיכת מזומן", "משיכה בבנקט", "הפק מזומן", "כספומט") -> RowKind.CASH_WITHDRAWAL
            has(text, "העברה", "העב'", "פייבוקס", "paybox", "זיכוי מבנק") || hasWord(text, "bit", "ביט") ->
                if (own) RowKind.OWN_TRANSFER else RowKind.TRANSFER
            has(text, "הוראת קבע", "הו\"ק") -> RowKind.STANDING_ORDER
            outflow -> RowKind.EXPENSE
            else -> RowKind.INCOME
        }
        Source.MAX, Source.CAL -> if (has(text, "הוראת קבע")) RowKind.STANDING_ORDER else RowKind.EXPENSE
    }
}

private val budgetKinds = setOf(RowKind.EXPENSE, RowKind.STANDING_ORDER, RowKind.CASH_WITHDRAWAL)

/** Card bills (issuer, billing month) that have their own purchase lines among [rows]. */
private fun detailedBills(rows: List<StoredTransaction>): Set<Pair<Issuer, YearMonth>> =
    rows.mapNotNull { s -> issuerOf(s.row.source)?.let { it to s.row.billingMonth } }.toSet()

/**
 * Counted expense lines for planVsActual. A card bill in the bank is left out when that card's purchases
 * were imported for the same billing month, and counted (uncategorised) until then so no money vanishes.
 * Category: the user's choice, else the merchant memory, else uncategorised.
 */
fun toTransactions(rows: List<StoredTransaction>, memory: ImportMemory = ImportMemory()): List<Transaction> {
    val detailed = detailedBills(rows)
    return rows.filter { it.row.status == RowStatus.COUNTED && it.row.charge != null }.mapNotNull { stored ->
        val row = stored.row
        val kind = classify(row, memory)
        val counted = when (kind) {
            in budgetKinds -> true
            RowKind.TRANSFER -> row.charge!!.agorot > 0
            RowKind.CARD_TOTAL -> issuerOf(row.description)?.let { it to row.billingMonth } !in detailed
            else -> false
        }
        if (!counted) return@mapNotNull null
        val method = when {
            issuerOf(row.source) != null || kind == RowKind.CARD_TOTAL -> PaymentMethod.CREDIT_CARD
            kind == RowKind.CASH_WITHDRAWAL || kind == RowKind.TRANSFER -> PaymentMethod.CASH
            else -> PaymentMethod.STANDING_ORDER
        }
        val category = if (kind == RowKind.CASH_WITHDRAWAL || kind == RowKind.CARD_TOTAL) stored.categoryId
        else stored.categoryId ?: suggestCategory(row.description, memory.merchants)
        Transaction(
            row.id, row.date, row.billingMonth, row.description, row.charge!!, method, row.sourceId, category,
            row.originalAmount, row.originalCurrency, row.installment,
        )
    }
}

/** Money in for [month]: salaries, allowances and incoming transfers not marked as the household's own. */
fun actualIncome(rows: List<StoredTransaction>, month: YearMonth, memory: ImportMemory = ImportMemory()): Money =
    Money(rows.filter { it.row.status == RowStatus.COUNTED && it.row.billingMonth == month && it.row.charge != null }
        .filter { classify(it.row, memory) in setOf(RowKind.INCOME, RowKind.TRANSFER) && it.row.charge!!.agorot < 0 }
        .sumOf { -it.row.charge!!.agorot })
