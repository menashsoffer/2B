package com.menash.myself.core.data.mapper

import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.Task
import com.menash.myself.core.data.legacy.models.TaskItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LegacyMappingTest {

    @Test
    fun legacySpaceToForWhom_mapsExactValues() {
        // mine & שלי -> ME
        assertEquals(Pair(ForWhom.ME, false), ModelMappers.legacySpaceToForWhom("mine"))
        assertEquals(Pair(ForWhom.ME, false), ModelMappers.legacySpaceToForWhom("שלי"))
        assertEquals(Pair(ForWhom.ME, false), ModelMappers.legacySpaceToForWhom(" MINE "))

        // wife, partner & של אשתי -> PARTNER
        assertEquals(Pair(ForWhom.PARTNER, false), ModelMappers.legacySpaceToForWhom("wife"))
        assertEquals(Pair(ForWhom.PARTNER, false), ModelMappers.legacySpaceToForWhom("partner"))
        assertEquals(Pair(ForWhom.PARTNER, false), ModelMappers.legacySpaceToForWhom("של אשתי"))
        assertEquals(Pair(ForWhom.PARTNER, false), ModelMappers.legacySpaceToForWhom(" PARTNER "))

        // shared & משותף -> BOTH
        assertEquals(Pair(ForWhom.BOTH, false), ModelMappers.legacySpaceToForWhom("shared"))
        assertEquals(Pair(ForWhom.BOTH, false), ModelMappers.legacySpaceToForWhom("משותף"))
        assertEquals(Pair(ForWhom.BOTH, false), ModelMappers.legacySpaceToForWhom(" SHARED "))

        // private -> ME + isPrivate = true
        assertEquals(Pair(ForWhom.ME, true), ModelMappers.legacySpaceToForWhom("private"))
        assertEquals(Pair(ForWhom.ME, true), ModelMappers.legacySpaceToForWhom(" PRIVATE "))

        // Unknown values -> MUST be ME (never BOTH!)
        assertEquals(Pair(ForWhom.ME, false), ModelMappers.legacySpaceToForWhom("unknown_space"))
        assertEquals(Pair(ForWhom.ME, false), ModelMappers.legacySpaceToForWhom(""))
        assertEquals(Pair(ForWhom.ME, false), ModelMappers.legacySpaceToForWhom(null))
        assertEquals(Pair(ForWhom.ME, false), ModelMappers.legacySpaceToForWhom("something_random"))
    }

    @Test
    fun dates_parseBothIsoAndSlashFormats() {
        // yyyy-MM-dd
        val iso = ModelMappers.parseLocalDate("2026-05-15")
        assertEquals(LocalDate.of(2026, 5, 15), iso)

        // dd/MM/yyyy
        val slash = ModelMappers.parseLocalDate("15/05/2026")
        assertEquals(LocalDate.of(2026, 5, 15), slash)

        val singleDigitSlash = ModelMappers.parseLocalDate("5/7/2026")
        assertEquals(LocalDate.of(2026, 7, 5), singleDigitSlash)
    }

    @Test
    fun dates_unparseable_keepsRawString_neverOverwritesWithEmptyOnSave() {
        val rawInput = "בסוכות 2026"
        val taskItem = TaskItem(
            id = "t_raw",
            title = "משימה עם תאריך טקסטואלי",
            dueDate = rawInput,
            space = "mine",
            householdId = "h1",
            userId = "u1"
        )

        // Map to domain
        val domainTask = TaskMappers.taskToDomain(taskItem)
        assertNull(domainTask.dueDate) // cannot be parsed as LocalDate
        assertEquals(rawInput, domainTask.rawDueDate) // raw string is preserved!

        // Map back to data (simulate save)
        val savedItem = TaskMappers.taskToData(domainTask, "h1")
        assertEquals(rawInput, savedItem.dueDate) // NEVER overwritten with empty!
    }
}
