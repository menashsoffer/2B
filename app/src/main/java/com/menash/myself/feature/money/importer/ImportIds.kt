package com.menash.myself.feature.money.importer

import com.menash.myself.feature.money.domain.normalizeMerchant
import java.security.MessageDigest

/**
 * Content-only transaction ids. The key never holds the file name, import time or row position, so the
 * same line in a re-uploaded or overlapping file gets the same id and is updated, never added twice.
 * Identical lines in one file (two coffees, same price, same day) get occurrence 0, 1, … in file order:
 * they stay two, and an overlapping file holding the same day maps onto the same two ids.
 */
object ImportIds {

    fun hash(parts: List<String>): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(parts.joinToString("\u0001").toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }

    fun merchantKey(description: String): String = normalizeMerchant(maskDigits(cleanText(description)))

    /** keys[i] is row i's content key; the result is the id per row, occurrence appended. */
    fun assign(prefix: String, keys: List<List<String>>): List<String> {
        val seen = mutableMapOf<List<String>, Int>()
        return keys.map { key ->
            val occurrence = seen[key] ?: 0
            seen[key] = occurrence + 1
            "tx-$prefix-" + hash(key + occurrence.toString())
        }
    }

    fun memoryDocId(merchantKey: String): String = "m-" + hash(listOf(merchantKey))
}
