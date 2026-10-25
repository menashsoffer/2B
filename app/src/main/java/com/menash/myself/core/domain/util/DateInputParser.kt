package com.menash.myself.core.domain.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateInputParser {
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val slashFormatter = DateTimeFormatter.ofPattern("d/M/yyyy")
    private val displaySlashFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun todayString(): String {
        return LocalDate.now().format(displaySlashFormatter)
    }

    fun parseOrNull(input: String?): LocalDate? {
        if (input.isNullOrBlank()) return null
        val trimmed = input.trim()
        try {
            return LocalDate.parse(trimmed, isoFormatter)
        } catch (_: Exception) {}
        try {
            return LocalDate.parse(trimmed, slashFormatter)
        } catch (_: Exception) {}
        return null
    }

    fun parseDateOrNull(input: String?): LocalDate? = parseOrNull(input)

    fun parseWithRaw(input: String?): Pair<LocalDate?, String?> {
        if (input.isNullOrBlank()) return Pair(null, null)
        val trimmed = input.trim()
        val parsed = parseOrNull(trimmed)
        return if (parsed != null) {
            Pair(parsed, null)
        } else {
            Pair(null, trimmed)
        }
    }

    fun formatIso(date: LocalDate?): String {
        return date?.format(isoFormatter) ?: ""
    }

    fun formatDate(date: LocalDate?, rawFallback: String? = null): String {
        return date?.format(isoFormatter) ?: (rawFallback ?: "")
    }
}
