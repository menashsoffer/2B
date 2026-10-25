package com.menash.myself.core.data.mapper

import com.google.firebase.Timestamp
import com.menash.myself.core.domain.model.AiSubscription
import com.menash.myself.core.domain.model.Car
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.FitnessEntry
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.Household
import com.menash.myself.core.domain.model.InboxItem
import com.menash.myself.core.domain.model.PensionFund
import com.menash.myself.core.domain.model.Task
import com.menash.myself.data.entities.AiSubscriptionEntity
import com.menash.myself.data.entities.CarEntity
import com.menash.myself.data.entities.PensionFundEntity
import com.menash.myself.data.models.DreamItem
import com.menash.myself.data.models.FitnessLog
import com.menash.myself.data.models.TaskItem
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date

/**
 * Shared legacy space/date conversion utilities and facade delegating to per-collection mappers:
 * [TaskMappers], [DreamMappers], [InboxMappers], [FitnessMappers], [HouseholdMappers], [CalculatorMappers].
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

    // Delegations to per-collection mappers
    fun carToDomain(entity: CarEntity): Car = CalculatorMappers.carToDomain(entity)
    fun carToEntity(domain: Car): CarEntity = CalculatorMappers.carToEntity(domain)
    fun aiSubscriptionToDomain(entity: AiSubscriptionEntity): AiSubscription =
        CalculatorMappers.aiSubscriptionToDomain(entity)
    fun aiSubscriptionToEntity(domain: AiSubscription): AiSubscriptionEntity =
        CalculatorMappers.aiSubscriptionToEntity(domain)
    fun pensionFundToDomain(entity: PensionFundEntity): PensionFund =
        CalculatorMappers.pensionFundToDomain(entity)
    fun pensionFundToEntity(domain: PensionFund): PensionFundEntity =
        CalculatorMappers.pensionFundToEntity(domain)

    fun taskToDomain(data: TaskItem): Task = TaskMappers.taskToDomain(data)
    fun taskToData(domain: Task, householdId: String): TaskItem = TaskMappers.taskToData(domain, householdId)

    fun dreamToDomain(data: DreamItem): Dream = DreamMappers.dreamToDomain(data)
    fun dreamToData(domain: Dream, householdId: String): DreamItem = DreamMappers.dreamToData(domain, householdId)

    fun inboxToDomain(data: com.menash.myself.data.models.InboxItem): InboxItem = InboxMappers.inboxToDomain(data)
    fun inboxToData(domain: InboxItem, householdId: String): com.menash.myself.data.models.InboxItem =
        InboxMappers.inboxToData(domain, householdId)

    fun fitnessToDomain(data: FitnessLog): FitnessEntry = FitnessMappers.fitnessToDomain(data)
    fun fitnessToData(domain: FitnessEntry): FitnessLog = FitnessMappers.fitnessToData(domain)

    fun householdToDomain(data: com.menash.myself.data.models.Household): Household =
        HouseholdMappers.householdToDomain(data)
    fun householdToData(domain: Household): com.menash.myself.data.models.Household =
        HouseholdMappers.householdToData(domain)
}
