package com.menash.myself.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.Timestamp

enum class SpaceType(val displayName: String, val id: String) {
    MINE("שלי", "mine"),
    WIFE("של אשתי", "wife"),
    SHARED("משותף", "shared");

    companion object {
        fun fromId(id: String): SpaceType {
            return entries.firstOrNull { it.id == id || it.displayName == id } ?: SHARED
        }
    }
}

data class Household(
    @DocumentId val id: String = "",
    val name: String = "הבית שלנו",
    val ownerUid: String = "",
    val ownerEmail: String = "",
    val partnerEmail: String = "",
    val partnerUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class InboxItem(
    @DocumentId val id: String = "",
    val text: String = "",
    val userId: String = "",
    val space: String = SpaceType.SHARED.id,
    val status: String = "inbox", // inbox, triaged
    val triagedType: String = "", // task, dream, idea, note
    val householdId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class DreamStatus(val displayName: String) {
    DREAM("חלום"),
    UNDER_REVIEW("בבדיקה"),
    SAVING_PLANNING("חוסכים/מתכננים"),
    DONE("בוצע"),
    ARCHIVED("נגנז")
}

data class DreamItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val list: String = "לבית", // לבית, טיסות משפחה, פיתוח ואפליקציות, או מותאם אישית
    val estimatedCost: Double = 0.0,
    val savedAmount: Double = 0.0,
    val link: String = "",
    val priority: Int = 2, // 1 (גבוה), 2 (בינוני), 3 (נמוך)
    val status: String = DreamStatus.DREAM.displayName,
    val targetDate: String = "",
    val notes: String = "",
    val space: String = SpaceType.SHARED.id,
    val userId: String = "",
    val householdId: String = "",
    val appIdeaQuestions: Map<String, String> = emptyMap(),
    val nextAction: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val progressPercent: Float
        get() = if (estimatedCost > 0) ((savedAmount / estimatedCost).coerceIn(0.0, 1.0)).toFloat() else 0f
}

data class TaskItem(
    @DocumentId val id: String = "",
    val title: String = "", // starts with a verb, e.g. "להזמין כרטיסים"
    val nextAction: String = "", // small next action step
    val isTop3: Boolean = false,
    val isFiveBefore11: Boolean = false,
    val dueDate: String = "", // YYYY-MM-DD or DD/MM/YYYY or empty
    val isDone: Boolean = false,
    val space: String = SpaceType.SHARED.id,
    val userId: String = "",
    val householdId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class FitnessLog(
    @DocumentId val id: String = "",
    val date: String = "", // YYYY-MM-DD
    val moved: Boolean = false,
    val minutes: Int = 0,
    val type: String = "הליכה", // הליכה, ריצה, אימון כוח, יוגה, שחייה, אופניים, אחר
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class CalendarEvent(
    val id: Long = 0,
    val title: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val location: String = "",
    val isAllDay: Boolean = false
)

data class CalculatorSync(
    @DocumentId val id: String = "",
    val calculatorType: String = "", // car, ai, pension
    val recordsCount: Int = 0,
    val dataJson: String = "",
    val userId: String = "",
    val space: String = SpaceType.SHARED.id,
    @ServerTimestamp val createdAt: Timestamp? = null
)
