package com.menash.myself.core.domain.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSearchTest {

    private fun q(text: String) = SearchQuery.parse(text)

    @Test fun `empty query matches everything`() {
        assertTrue(q("   ").isEmpty)
        assertTrue(q("").matches(listOf("anything")))
    }

    @Test fun `case and spaces are ignored`() {
        assertTrue(q("  NETFLIX  ").matches(listOf("Netflix.com")))
    }

    @Test fun `final letters match their regular form both ways`() {
        assertTrue(q("שופרסל דיל").matches(listOf("שופרסל דיל ת\"א")))
        assertTrue(q("מסעדת חומוס").matches(listOf("מסעדת חומוס אליהו")))
        assertTrue(q("חומו").matches(listOf("חומוס")))
        assertEquals(TextSearch.normalize("כסף"), TextSearch.normalize("כספ"))
    }

    @Test fun `niqqud and quote marks are ignored`() {
        assertTrue(q("בית\"ר").matches(listOf("ביתר עילית")))
        assertTrue(q("שָׁלוֹם").matches(listOf("שלום")))
    }

    @Test fun `every word must match, in any field`() {
        assertTrue(q("סופר מזון").matches(listOf("סופר יודה", "מזון")))
        assertFalse(q("סופר דלק").matches(listOf("סופר יודה", "מזון")))
    }

    @Test fun `a number without decimals matches within one shekel`() {
        assertTrue(q("120").matches(listOf("רמי לוי"), listOf(12_050)))
        assertTrue(q("120").matches(listOf("רמי לוי"), listOf(-11_900)))
        assertFalse(q("120").matches(listOf("רמי לוי"), listOf(12_101)))
    }

    @Test fun `a number with decimals matches exactly`() {
        assertTrue(q("120.5").matches(listOf("x"), listOf(12_050)))
        assertFalse(q("120.50").matches(listOf("x"), listOf(12_000)))
    }

    @Test fun `shekel sign and thousands comma are accepted`() {
        assertTrue(q("₪1,200").matches(listOf("ביטוח"), listOf(120_000)))
    }

    @Test fun `a number can also match text`() {
        assertTrue(q("10").matches(listOf("סניף 10"), emptyList()))
    }

    @Test fun `text and amount combine`() {
        assertTrue(q("דלק 300").matches(listOf("פז דלק"), listOf(30_000)))
        assertFalse(q("דלק 300").matches(listOf("פז דלק"), listOf(50_000)))
    }

    @Test fun `search keeps order and filters`() {
        val items = listOf("שופרסל" to 100L, "פז" to 30_000L, "שופרסל אונליין" to 5_000L)
        val found = items.search(q("שופרסל"), texts = { listOf(it.first) }, amounts = { listOf(it.second) })
        assertEquals(listOf("שופרסל", "שופרסל אונליין"), found.map { it.first })
    }
}
