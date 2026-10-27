package com.menash.myself.ui.home

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.ULocale
import androidx.annotation.StringRes
import com.menash.myself.R

@StringRes
fun greetingFor(hour: Int): Int = when (hour) {
    in 5..11 -> R.string.greeting_morning
    in 12..16 -> R.string.greeting_afternoon
    else -> R.string.greeting_evening
}

fun hourOf(epochMillis: Long): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = epochMillis
    return calendar.get(Calendar.HOUR_OF_DAY)
}

fun firstNameOf(displayName: String?): String? {
    val trimmed = displayName?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    return trimmed.split(Regex("\\s+")).firstOrNull()?.takeIf { it.isNotEmpty() }
}

fun homeTitle(displayName: String?, greeting: String): String {
    val firstName = firstNameOf(displayName)
    return if (firstName == null) greeting else "$greeting, $firstName"
}

fun hebrewDateLine(epochMillis: Long): String {
    val locale = ULocale("he_IL")
    val calendar = Calendar.getInstance(locale)
    calendar.timeInMillis = epochMillis
    val format = SimpleDateFormat("EEEE, d 'ב'MMMM", locale)
    format.timeZone = calendar.timeZone
    return format.format(calendar.time)
}
