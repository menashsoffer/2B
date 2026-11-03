package com.menash.myself.core.domain.search

import java.math.BigDecimal

/**
 * Generic, screen-free search used by every list in the app (money lines today; plan items, tasks or
 * shopping later). A screen supplies, per item, the texts it can be found by and, optionally, its amounts
 * in agorot. Hebrew is matched loosely: niqqud, geresh/gershayim and quotes are ignored, and the final
 * letters ך ם ן ף ץ equal כ מ נ פ צ, so "סופר" finds "סופרפארם" and "שופרסל" finds "שופרסל דיל".
 */
object TextSearch {
    private val niqqud = Regex("[\\u0591-\\u05C7]")
    private val marks = Regex("[\"'`׳״]")
    private val spaces = Regex("\\s+")
    private val finals = mapOf('ך' to 'כ', 'ם' to 'מ', 'ן' to 'נ', 'ף' to 'פ', 'ץ' to 'צ')

    /** Lower case, no niqqud or quote marks, final letters folded, single spaces. */
    fun normalize(text: String): String =
        text.lowercase()
            .replace(niqqud, "")
            .replace(marks, "")
            .map { finals[it] ?: it }
            .joinToString("")
            .replace(spaces, " ")
            .trim()
}

/** One word the user typed; [amount] is set when the word also reads as a sum of money. */
data class SearchTerm(val text: String, val amount: SearchAmount? = null)

/**
 * A typed sum in agorot. Without decimals ("120") it matches ±1 ₪, since bank lines often carry agorot
 * the user does not remember; with decimals ("120.50") it matches exactly. Signs are ignored.
 */
data class SearchAmount(val agorot: Long, val exact: Boolean) {
    fun matches(value: Long): Boolean {
        val diff = kotlin.math.abs(kotlin.math.abs(value) - agorot)
        return if (exact) diff == 0L else diff <= 100L
    }
}

/** A parsed search box. Every term must match an item; an empty query matches everything. */
data class SearchQuery(val terms: List<SearchTerm>) {
    val isEmpty: Boolean get() = terms.isEmpty()

    /** True when every term is found in one of [texts] or, for a number, equals one of [amounts]. */
    fun matches(texts: List<String?>, amounts: List<Long> = emptyList()): Boolean {
        if (isEmpty) return true
        val haystack = texts.filterNotNull().joinToString(" ") { TextSearch.normalize(it) }
        return terms.all { term ->
            haystack.contains(term.text) || (term.amount != null && amounts.any(term.amount::matches))
        }
    }

    companion object {
        private val number = Regex("^\\d{1,9}(\\.\\d{1,2})?$")

        fun parse(input: String): SearchQuery = SearchQuery(
            TextSearch.normalize(input).split(' ').filter { it.isNotEmpty() }.map { word ->
                SearchTerm(word, amountOf(word))
            },
        )

        private fun amountOf(word: String): SearchAmount? {
            val clean = word.replace("₪", "").replace(",", "")
            if (!number.matches(clean)) return null
            val agorot = BigDecimal(clean).movePointRight(2).toLong()
            return SearchAmount(agorot, exact = clean.contains('.'))
        }
    }
}

/** Keeps the items [query] matches, in their order. [texts] and [amounts] say how one item is found. */
fun <T> List<T>.search(query: SearchQuery, texts: (T) -> List<String?>, amounts: (T) -> List<Long> = { emptyList() }): List<T> =
    if (query.isEmpty) this else filter { query.matches(texts(it), amounts(it)) }
