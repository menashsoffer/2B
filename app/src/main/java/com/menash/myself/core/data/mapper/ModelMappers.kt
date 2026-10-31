package com.menash.myself.core.data.mapper

import com.google.firebase.Timestamp
import com.menash.myself.core.domain.model.ForWhom
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date

/**
 * Shared legacy space/date conversion utilities used by the per-collection mappers:
 * [TaskMappers], [DreamMappers], [InboxMappers], [FitnessMappers], [HouseholdMappers].
 */
object ModelMappers {

    private val isoDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val slashDateFormatter = DateTimeFormatter.ofPattern("d/M/yyyy")

    var lastLoggedWarning: String? = null
        private set

    private fun logWarning(message: String) {
        lastLoggedWarning = message
        runCatching { android.util.Log.w("ModelMappers", message) }
    }

    fun forWhomToLegacySpace(forWhom: ForWhom, isPrivate: Boolean): String {
        if (isPrivate) return "private"
        return when (forWhom) {
            ForWhom.BOTH -> "shared"
            ForWhom.ME -> "mine"
            ForWhom.PARTNER -> "partner"
        }
    }

    fun legacySpaceToForWhom(space: String?): Pair<ForWhom, Boolean> {
        val clean = space?.trim()?.lowercase() ?: ""
        return when (clean) {
            "private" -> Pair(ForWhom.ME, true)
            "mine", "שלי" -> Pair(ForWhom.ME, false)
            "wife", "partner", "של אשתי" -> Pair(ForWhom.PARTNER, false)
            "shared", "משותף" -> Pair(ForWhom.BOTH, false)
            else -> {
                logWarning("Unknown space value: '$space', defaulting to ME (never BOTH)")
                Pair(ForWhom.ME, false)
            }
        }
    }

    fun parseLocalDate(dateStr: String?): LocalDate? = parseLocalDateWithRaw(dateStr).first

    fun parseLocalDateWithRaw(dateStr: String?): Pair<LocalDate?, String?> {
        if (dateStr.isNullOrBlank()) return Pair(null, null)
        val trimmed = dateStr.trim()
        try {
            return Pair(LocalDate.parse(trimmed, isoDateFormatter), null)
        } catch (_: Exception) {}
        try {
            return Pair(LocalDate.parse(trimmed, slashDateFormatter), null)
        } catch (_: Exception) {}
        logWarning("Unparseable legacy date: '$dateStr', preserving raw string")
        return Pair(null, trimmed)
    }

    fun formatLocalDate(date: LocalDate?): String = date?.format(isoDateFormatter) ?: ""

    fun timestampToInstant(timestamp: Timestamp?): Instant? = timestamp?.toDate()?.toInstant()

    fun instantToTimestamp(instant: Instant?): Timestamp? = instant?.let { Timestamp(Date.from(it)) }
}
