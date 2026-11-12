package com.menash.myself.core.domain.model

import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.Task
import com.menash.myself.core.domain.model.VisibilityFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisibilityFilterTest {

    @Test
    fun visibilityFilter_mineAndBoth_includesOnlyMeAndBoth() {
        val tasks = listOf(
            Task(id = "1", title = "משימה שלי", forWhom = ForWhom.ME),
            Task(id = "2", title = "משימה של בת הזוג", forWhom = ForWhom.PARTNER),
            Task(id = "3", title = "משימה של שנינו", forWhom = ForWhom.BOTH)
        )

        val filtered = tasks.filter { task ->
            when (VisibilityFilter.MINE_AND_BOTH) {
                VisibilityFilter.ALL -> true
                VisibilityFilter.MINE_AND_BOTH -> task.forWhom == ForWhom.ME || task.forWhom == ForWhom.BOTH
            }
        }

        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.id == "1" })
        assertTrue(filtered.any { it.id == "3" })
        assertFalse(filtered.any { it.id == "2" })
    }

    @Test
    fun visibilityFilter_all_includesAllHouseholdItems() {
        val tasks = listOf(
            Task(id = "1", title = "משימה שלי", forWhom = ForWhom.ME),
            Task(id = "2", title = "משימה של בת הזוג", forWhom = ForWhom.PARTNER),
            Task(id = "3", title = "משימה של שנינו", forWhom = ForWhom.BOTH)
        )

        val filtered = tasks.filter { task ->
            when (VisibilityFilter.ALL) {
                VisibilityFilter.ALL -> true
                VisibilityFilter.MINE_AND_BOTH -> task.forWhom == ForWhom.ME || task.forWhom == ForWhom.BOTH
            }
        }

        assertEquals(3, filtered.size)
    }
}
