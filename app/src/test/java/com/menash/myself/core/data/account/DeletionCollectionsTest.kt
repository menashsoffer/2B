package com.menash.myself.core.data.account

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** A household collection the rules allow but deleteHousehold does not drain would survive a "deleted" household. */
class DeletionCollectionsTest {

    private val rules = File(File("..").canonicalFile, "firestore.rules").readText()

    @Test
    fun moneyDeletionListMatchesTheRules() {
        val list = Regex("""function isMoneyCollection\(name\)\s*\{\s*return name in \[(.*?)]""", RegexOption.DOT_MATCHES_ALL)
            .find(rules)?.groupValues?.get(1) ?: error("isMoneyCollection not found in firestore.rules")
        val inRules = Regex("'([A-Za-z]+)'").findAll(list).map { it.groupValues[1] }.toSet()
        assertEquals(inRules, DeletionCollections.money.toSet())
    }

    @Test
    fun everyNamedHouseholdSubcollectionIsDeleted() {
        val block = rules.substringAfter("match /households/{householdId} {").substringBefore("match /users/{userId}")
        val named = Regex("""match /([A-Za-z]+)/\{""").findAll(block).map { it.groupValues[1] }.toSet()
        assertEquals(setOf("tasks", "dreams", "inbox"), named)
        assertEquals(DeletionCollections.household.toSet(), DeletionCollections.money.toSet() + named)
    }
}
