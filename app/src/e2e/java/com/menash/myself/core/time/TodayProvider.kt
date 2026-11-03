package com.menash.myself.core.time

import java.time.LocalDate

/** Process-wide test clock; this source set is excluded from release. */
object TodayProvider {
    @Volatile private var fixedDate: LocalDate? = null
    fun today(): LocalDate = fixedDate ?: defaultToday()
    fun setForE2e(date: LocalDate) { fixedDate = date }
}
