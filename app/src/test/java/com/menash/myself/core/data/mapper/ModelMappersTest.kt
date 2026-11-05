package com.menash.myself.core.data.mapper

import com.google.firebase.Timestamp
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.FitnessEntry
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.Household
import com.menash.myself.core.domain.model.InboxItem
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.model.Task
import com.menash.myself.core.data.legacy.models.DreamItem
import com.menash.myself.core.data.legacy.models.FitnessLog
import com.menash.myself.core.data.legacy.models.TaskItem
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.Date

class ModelMappersTest {

    @Test
    fun legacySpace_mappings_roundTrips() {
        // Domain to Legacy Space
        assertEquals("shared", ModelMappers.forWhomToLegacySpace(ForWhom.BOTH, isPrivate = false))
        assertEquals("mine", ModelMappers.forWhomToLegacySpace(ForWhom.ME, isPrivate = false))
        assertEquals("partner", ModelMappers.forWhomToLegacySpace(ForWhom.PARTNER, isPrivate = false))
        assertEquals("private", ModelMappers.forWhomToLegacySpace(ForWhom.ME, isPrivate = true))
        assertEquals("private", ModelMappers.forWhomToLegacySpace(ForWhom.BOTH, isPrivate = true))

        // Legacy Space to Domain
        assertEquals(Pair(ForWhom.BOTH, false), ModelMappers.legacySpaceToForWhom("shared"))
        assertEquals(Pair(ForWhom.ME, false), ModelMappers.legacySpaceToForWhom("mine"))
        assertEquals(Pair(ForWhom.PARTNER, false), ModelMappers.legacySpaceToForWhom("partner"))
        assertEquals(Pair(ForWhom.ME, true), ModelMappers.legacySpaceToForWhom("private"))
        assertEquals(Pair(ForWhom.ME, false), ModelMappers.legacySpaceToForWhom("unknown"))
        assertEquals(Pair(ForWhom.ME, false), ModelMappers.legacySpaceToForWhom(null))
    }

    @Test
    fun task_roundTrip() {
        val now = Date()
        val data = TaskItem(
            id = "task_42",
            title = "לקנות חלב",
            nextAction = "ללכת לסופר",
            isDone = false,
            isTop3 = true,
            isFiveBefore11 = false,
            dueDate = "2026-10-30",
            space = "mine",
            userId = "user_1",
            householdId = "house_1",
            createdAt = Timestamp(now)
        )

        val domain = TaskMappers.taskToDomain(data)
        assertEquals("task_42", domain.id)
        assertEquals("לקנות חלב", domain.title)
        assertEquals(LocalDate.of(2026, 10, 30), domain.dueDate)
        assertEquals(ForWhom.ME, domain.forWhom)
        assertFalse(domain.isPrivate)
        assertEquals("user_1", domain.ownerUid)

        val roundTripData = TaskMappers.taskToData(domain, "house_1")
        assertEquals(data.id, roundTripData.id)
        assertEquals(data.title, roundTripData.title)
        assertEquals("2026-10-30", roundTripData.dueDate)
        assertEquals("mine", roundTripData.space)
        assertEquals("user_1", roundTripData.userId)
    }

    @Test
    fun dream_roundTrip() {
        val data = DreamItem(
            id = "dream_10",
            title = "חופשה ביפן",
            list = "חלומות גדולים",
            estimatedCost = 35000.0,
            savedAmount = 12500.0,
            link = "https://japan.travel",
            priority = 2,
            status = "active",
            targetDate = "2027-04-15",
            notes = "עונת פריחת הדובדבן",
            space = "shared",
            userId = "user_2",
            householdId = "house_1"
        )

        val domain = DreamMappers.dreamToDomain(data)
        assertEquals(35000.0, domain.estimatedCost.shekels, 0.001)
        assertEquals(3500000L, domain.estimatedCost.agorot)
        assertEquals(12500.0, domain.savedAmount.shekels, 0.001)
        assertEquals(LocalDate.of(2027, 4, 15), domain.targetDate)
        assertEquals(ForWhom.BOTH, domain.forWhom)

        val roundTripData = DreamMappers.dreamToData(domain, "house_1")
        assertEquals(data.id, roundTripData.id)
        assertEquals(data.estimatedCost, roundTripData.estimatedCost, 0.001)
        assertEquals("shared", roundTripData.space)
        assertEquals("2027-04-15", roundTripData.targetDate)
    }

    @Test
    fun fitness_roundTrip() {
        val data = FitnessLog(
            id = "2026-10-25",
            date = "2026-10-25",
            moved = true,
            minutes = 45,
            type = "ריצה",
            userId = "user_1"
        )

        val domain = FitnessMappers.fitnessToDomain(data)
        assertEquals(LocalDate.of(2026, 10, 25), domain.date)
        assertTrue(domain.moved)
        assertEquals(45, domain.minutes)

        val roundTripData = FitnessMappers.fitnessToData(domain)
        assertEquals("2026-10-25", roundTripData.date)
        assertEquals(45, roundTripData.minutes)
    }

    @Test
    fun household_roundTrip() {
        val data = com.menash.myself.core.data.legacy.models.Household(
            id = "h1",
            name = "בית משפחת כהן",
            ownerUid = "alice",
            memberUids = listOf("alice", "bob"),
            partnerEmail = "bob@example.com",
            partnerUid = "bob"
        )

        val domain = HouseholdMappers.householdToDomain(data)
        assertEquals("h1", domain.id)
        assertEquals("בית משפחת כהן", domain.name)
        assertTrue(domain.isShared)

        val roundTripData = HouseholdMappers.householdToData(domain)
        assertEquals(data.id, roundTripData.id)
        assertEquals(data.name, roundTripData.name)
        assertEquals(data.partnerEmail, roundTripData.partnerEmail)
    }
}
