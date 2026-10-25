package com.menash.myself.core.domain.model

/**
 * Typed domain representation for choosing an item's audience and privacy in the UI.
 * Avoids any legacy string ("mine"/"shared"/"wife"/"private") in feature/ or ui/.
 */
enum class SpaceSelection(
    val displayName: String,
    val forWhom: ForWhom,
    val isPrivate: Boolean
) {
    SHARED("משותף", ForWhom.BOTH, false),
    MINE("שלי", ForWhom.ME, false),
    PARTNER("של אשתי", ForWhom.PARTNER, false),
    PRIVATE("פרטי", ForWhom.ME, true);

    companion object {
        fun from(forWhom: ForWhom, isPrivate: Boolean): SpaceSelection {
            if (isPrivate) return PRIVATE
            return when (forWhom) {
                ForWhom.BOTH -> SHARED
                ForWhom.ME -> MINE
                ForWhom.PARTNER -> PARTNER
            }
        }

        fun parseFromCsv(value: String?): SpaceSelection {
            val clean = value?.trim()?.lowercase() ?: ""
            return when (clean) {
                "private", "פרטי" -> PRIVATE
                "mine", "שלי", "me" -> MINE
                "wife", "partner", "של אשתי", "בן/בת הזוג" -> PARTNER
                "shared", "משותף", "both", "של שנינו" -> SHARED
                else -> MINE
            }
        }
    }
}
