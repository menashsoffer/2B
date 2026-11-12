package com.menash.myself.feature.home.ui

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.ULocale
import java.time.LocalDate

enum class GreetingVisit { First, Returning }

data class GreetingRecord(val firstDate: LocalDate? = null, val lastShown: LocalDate? = null)

/** First calendar day says hello; every later day welcomes the user back. */
fun greetingVisit(record: GreetingRecord, today: LocalDate): GreetingVisit =
    if (record.firstDate == null || record.firstDate == today) GreetingVisit.First else GreetingVisit.Returning

/** Animate only the first time the greeting is shown on a calendar day, and never with reduced motion. */
fun shouldAnimateGreeting(record: GreetingRecord, today: LocalDate, reducedMotion: Boolean): Boolean =
    !reducedMotion && record.lastShown != today

/** Persist at the moment the greeting is shown, so a rotation or a later visit the same day stays still. */
fun markGreetingShown(record: GreetingRecord, today: LocalDate): GreetingRecord =
    GreetingRecord(firstDate = record.firstDate ?: today, lastShown = today)

/** First token only. A long name keeps 15 characters and an ellipsis. A Latin name is isolated. */
fun greetingName(displayName: String?): String? {
    val token = firstNameOf(displayName) ?: return null
    val shown = if (token.length > 16) token.take(15) + "…" else token
    val latin = shown.any { it in 'A'..'Z' || it in 'a'..'z' }
    return if (latin) "\u2068$shown\u2069" else shown
}

fun greetingLine(words: String, displayName: String?): String {
    val name = greetingName(displayName) ?: return words
    return "$words, $name"
}

fun firstNameOf(displayName: String?): String? {
    val trimmed = displayName?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    return trimmed.split(Regex("\\s+")).firstOrNull()?.takeIf { it.isNotEmpty() }
}

fun hebrewDateLine(epochMillis: Long): String {
    val locale = ULocale("he_IL")
    val calendar = Calendar.getInstance(locale)
    calendar.timeInMillis = epochMillis
    val format = SimpleDateFormat("EEEE, d 'ב'MMMM", locale)
    format.timeZone = calendar.timeZone
    return format.format(calendar.time)
}
