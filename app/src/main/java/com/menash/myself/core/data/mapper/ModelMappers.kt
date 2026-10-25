package com.menash.myself.core.data.mapper

import com.google.firebase.Timestamp
import com.menash.myself.core.domain.model.AiSubscription
import com.menash.myself.core.domain.model.Car
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.FitnessEntry
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.Household
import com.menash.myself.core.domain.model.InboxItem
import com.menash.myself.core.domain.model.Money
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

object ModelMappers {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // --- CAR ---
    fun carToDomain(entity: CarEntity): Car {
        return Car(
            id = entity.id,
            name = entity.name,
            purchasePrice = Money.fromShekels(entity.purchasePrice),
            resaleValueAfter5Years = Money.fromShekels(entity.resaleValueAfter5Years),
            energyCostYearly = Money.fromShekels(entity.energyCostYearly),
            insuranceYearly = Money.fromShekels(entity.insuranceYearly),
            trackerYearly = Money.fromShekels(entity.trackerYearly),
            maintenanceYearly = Money.fromShekels(entity.maintenanceYearly),
            annualTestYearly = Money.fromShekels(entity.annualTestYearly),
            tires5YearsTotal = Money.fromShekels(entity.tires5YearsTotal),
            isBaseline = entity.isBaseline
        )
    }

    fun carToEntity(domain: Car): CarEntity {
        return CarEntity(
            id = domain.id,
            name = domain.name,
            purchasePrice = domain.purchasePrice.shekels,
            resaleValueAfter5Years = domain.resaleValueAfter5Years.shekels,
            energyCostYearly = domain.energyCostYearly.shekels,
            insuranceYearly = domain.insuranceYearly.shekels,
            trackerYearly = domain.trackerYearly.shekels,
            maintenanceYearly = domain.maintenanceYearly.shekels,
            annualTestYearly = domain.annualTestYearly.shekels,
            tires5YearsTotal = domain.tires5YearsTotal.shekels,
            isBaseline = domain.isBaseline
        )
    }

    // --- AI SUBSCRIPTION ---
    fun aiSubscriptionToDomain(entity: AiSubscriptionEntity): AiSubscription {
        return AiSubscription(
            id = entity.id,
            name = entity.name,
            category = entity.category,
            isActive = entity.isActive,
            billingCycle = entity.billingCycle,
            currency = entity.currency,
            price = Money.fromShekels(entity.price),
            notes = entity.notes
        )
    }

    fun aiSubscriptionToEntity(domain: AiSubscription): AiSubscriptionEntity {
        return AiSubscriptionEntity(
            id = domain.id,
            name = domain.name,
            category = domain.category,
            isActive = domain.isActive,
            billingCycle = domain.billingCycle,
            currency = domain.currency,
            price = domain.price.shekels,
            notes = domain.notes
        )
    }

    // --- PENSION FUND ---
    fun pensionFundToDomain(entity: PensionFundEntity): PensionFund {
        return PensionFund(
            id = entity.id,
            fundType = entity.fundType,
            provider = entity.provider,
            currentValue = Money.fromShekels(entity.currentValue),
            totalDeposits = Money.fromShekels(entity.totalDeposits),
            feeOnBalancePercent = entity.feeOnBalancePercent,
            feeOnDepositsPercent = entity.feeOnDepositsPercent,
            notes = entity.notes
        )
    }

    fun pensionFundToEntity(domain: PensionFund): PensionFundEntity {
        return PensionFundEntity(
            id = domain.id,
            fundType = domain.fundType,
            provider = domain.provider,
            currentValue = domain.currentValue.shekels,
            totalDeposits = domain.totalDeposits.shekels,
            feeOnBalancePercent = domain.feeOnBalancePercent,
            feeOnDepositsPercent = domain.feeOnDepositsPercent,
            notes = domain.notes
        )
    }

    private val isoDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val slashDateFormatter = DateTimeFormatter.ofPattern("d/M/yyyy")

    // --- FORWHOM & LEGACY SPACE ---
    fun forWhomToLegacySpace(forWhom: ForWhom, isPrivate: Boolean): String {
        if (isPrivate) return "private"
        return when (forWhom) {
            ForWhom.BOTH -> "shared"
            ForWhom.ME -> "mine"
            ForWhom.PARTNER -> "partner"
        }
    }

    var lastLoggedWarning: String? = null
        private set

    private fun logWarning(message: String) {
        lastLoggedWarning = message
        runCatching { android.util.Log.w("ModelMappers", message) }
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

    // --- DATE CONVERSIONS ---
    fun parseLocalDate(dateStr: String?): LocalDate? {
        return parseLocalDateWithRaw(dateStr).first
    }

    fun parseLocalDateWithRaw(dateStr: String?): Pair<LocalDate?, String?> {
        if (dateStr.isNullOrBlank()) return Pair(null, null)
        val trimmed = dateStr.trim()
        // 1. Try ISO_LOCAL_DATE (yyyy-MM-dd)
        try {
            return Pair(LocalDate.parse(trimmed, isoDateFormatter), null)
        } catch (_: Exception) {}

        // 2. Try dd/MM/yyyy (or d/M/yyyy)
        try {
            return Pair(LocalDate.parse(trimmed, slashDateFormatter), null)
        } catch (_: Exception) {}

        // 3. Unparseable -> keep raw string, never overwrite with empty
        logWarning("Unparseable legacy date: '$dateStr', preserving raw string")
        return Pair(null, trimmed)
    }

    fun formatLocalDate(date: LocalDate?): String {
        return date?.format(isoDateFormatter) ?: ""
    }

    fun timestampToInstant(timestamp: Timestamp?): Instant? {
        return timestamp?.toDate()?.toInstant()
    }

    fun instantToTimestamp(instant: Instant?): Timestamp? {
        return instant?.let { Timestamp(Date.from(it)) }
    }

    // --- TASK ---
    fun taskToDomain(data: TaskItem): Task {
        val (forWhom, isPrivate) = legacySpaceToForWhom(data.space)
        val (parsedDate, rawDate) = parseLocalDateWithRaw(data.dueDate)
        return Task(
            id = data.id,
            title = data.title,
            nextAction = data.nextAction,
            isDone = data.isDone,
            isTop3 = data.isTop3,
            isFiveBefore11 = data.isFiveBefore11,
            dueDate = parsedDate,
            rawDueDate = rawDate,
            ownerUid = data.userId,
            forWhom = forWhom,
            isPrivate = isPrivate,
            createdAt = timestampToInstant(data.createdAt)
        )
    }

    fun taskToData(domain: Task, householdId: String): TaskItem {
        val effectiveDueDate = if (domain.dueDate != null) {
            formatLocalDate(domain.dueDate)
        } else {
            domain.rawDueDate ?: ""
        }
        return TaskItem(
            id = domain.id,
            title = domain.title,
            nextAction = domain.nextAction,
            isDone = domain.isDone,
            isTop3 = domain.isTop3,
            isFiveBefore11 = domain.isFiveBefore11,
            dueDate = effectiveDueDate,
            space = forWhomToLegacySpace(domain.forWhom, domain.isPrivate),
            userId = domain.ownerUid,
            householdId = householdId,
            createdAt = instantToTimestamp(domain.createdAt)
        )
    }

    // --- DREAM ---
    fun dreamToDomain(data: DreamItem): Dream {
        val (forWhom, isPrivate) = legacySpaceToForWhom(data.space)
        val (parsedDate, rawDate) = parseLocalDateWithRaw(data.targetDate)
        return Dream(
            id = data.id,
            title = data.title,
            list = data.list,
            estimatedCost = Money.fromShekels(data.estimatedCost),
            savedAmount = Money.fromShekels(data.savedAmount),
            link = data.link,
            priority = data.priority,
            status = data.status,
            targetDate = parsedDate,
            rawTargetDate = rawDate,
            notes = data.notes,
            ownerUid = data.userId,
            forWhom = forWhom,
            isPrivate = isPrivate,
            appIdeaQuestions = data.appIdeaQuestions,
            nextAction = data.nextAction,
            createdAt = timestampToInstant(data.createdAt)
        )
    }

    fun dreamToData(domain: Dream, householdId: String): DreamItem {
        val effectiveTargetDate = if (domain.targetDate != null) {
            formatLocalDate(domain.targetDate)
        } else {
            domain.rawTargetDate ?: ""
        }
        return DreamItem(
            id = domain.id,
            title = domain.title,
            list = domain.list,
            estimatedCost = domain.estimatedCost.shekels,
            savedAmount = domain.savedAmount.shekels,
            link = domain.link,
            priority = domain.priority,
            status = domain.status,
            targetDate = effectiveTargetDate,
            notes = domain.notes,
            space = forWhomToLegacySpace(domain.forWhom, domain.isPrivate),
            userId = domain.ownerUid,
            householdId = householdId,
            appIdeaQuestions = domain.appIdeaQuestions,
            nextAction = domain.nextAction,
            createdAt = instantToTimestamp(domain.createdAt)
        )
    }

    // --- INBOX ---
    fun inboxToDomain(data: com.menash.myself.data.models.InboxItem): InboxItem {
        val (forWhom, isPrivate) = legacySpaceToForWhom(data.space)
        return InboxItem(
            id = data.id,
            text = data.text,
            status = data.status,
            triagedType = data.triagedType,
            ownerUid = data.userId,
            forWhom = forWhom,
            isPrivate = isPrivate,
            createdAt = timestampToInstant(data.createdAt)
        )
    }

    fun inboxToData(domain: InboxItem, householdId: String): com.menash.myself.data.models.InboxItem {
        return com.menash.myself.data.models.InboxItem(
            id = domain.id,
            text = domain.text,
            space = forWhomToLegacySpace(domain.forWhom, domain.isPrivate),
            status = domain.status,
            triagedType = domain.triagedType,
            userId = domain.ownerUid,
            householdId = householdId,
            createdAt = instantToTimestamp(domain.createdAt)
        )
    }

    // --- FITNESS ---
    fun fitnessToDomain(data: FitnessLog): FitnessEntry {
        val date = parseLocalDate(data.date) ?: LocalDate.now()
        return FitnessEntry(
            id = data.id,
            date = date,
            moved = data.moved,
            minutes = data.minutes,
            type = data.type,
            ownerUid = data.userId,
            createdAt = timestampToInstant(data.createdAt)
        )
    }

    fun fitnessToData(domain: FitnessEntry): FitnessLog {
        return FitnessLog(
            id = domain.id,
            date = formatLocalDate(domain.date),
            moved = domain.moved,
            minutes = domain.minutes,
            type = domain.type,
            userId = domain.ownerUid,
            createdAt = instantToTimestamp(domain.createdAt)
        )
    }

    // --- HOUSEHOLD ---
    fun householdToDomain(data: com.menash.myself.data.models.Household): Household {
        return Household(
            id = data.id,
            name = data.name,
            ownerUid = data.ownerUid,
            memberUids = data.memberUids,
            partnerEmail = data.partnerEmail,
            partnerUid = data.partnerUid
        )
    }

    fun householdToData(domain: Household): com.menash.myself.data.models.Household {
        return com.menash.myself.data.models.Household(
            id = domain.id,
            name = domain.name,
            ownerUid = domain.ownerUid,
            memberUids = domain.memberUids,
            partnerEmail = domain.partnerEmail,
            partnerUid = domain.partnerUid
        )
    }
}
