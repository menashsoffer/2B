package com.menash.myself.core.domain.model

/**
 * Domain representation of item target audience within a shared household.
 */
enum class ForWhom {
    ME,
    PARTNER,
    BOTH;

    /**
     * Computes the display label for a viewer.
     *
     * Rules:
     * - If viewer is owner:
     *     ME -> "שלי"
     *     PARTNER -> partnerName (or "בן/בת הזוג")
     *     BOTH -> "של שנינו"
     * - If viewer is NOT owner (partner viewing owner's items):
     *     ME (owner's ME) -> partnerName (the owner's name)
     *     PARTNER (owner created for partner) -> "שלי" (it belongs to the viewer!)
     *     BOTH -> "של שנינו"
     */
    fun labelForViewer(
        isOwner: Boolean,
        partnerName: String = "בן/בת הזוג",
        mineLabel: String = "שלי",
        bothLabel: String = "של שנינו"
    ): String {
        val effectivePartnerName = partnerName.trim().ifEmpty { "בן/בת הזוג" }
        return if (isOwner) {
            when (this) {
                ME -> mineLabel
                PARTNER -> effectivePartnerName
                BOTH -> bothLabel
            }
        } else {
            when (this) {
                ME -> effectivePartnerName
                PARTNER -> mineLabel
                BOTH -> bothLabel
            }
        }
    }

    /**
     * Determines whether this item should be included in the viewer's active feed.
     *
     * Cognitive load reduction rule:
     * Default view displays "mine + both" (items belonging to viewer or shared).
     * When showAll is true, all items are displayed.
     */
    fun isVisibleToViewer(isOwner: Boolean, showAll: Boolean = false): Boolean {
        if (showAll) return true
        return if (isOwner) {
            this == ME || this == BOTH
        } else {
            this == PARTNER || this == BOTH
        }
    }

    companion object {
        fun fromString(value: String?): ForWhom {
            return when (value?.uppercase()?.trim()) {
                "PARTNER" -> PARTNER
                "BOTH" -> BOTH
                else -> ME
            }
        }
    }
}
