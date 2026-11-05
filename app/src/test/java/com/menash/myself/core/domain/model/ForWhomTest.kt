package com.menash.myself.core.domain.model

import com.menash.myself.core.domain.model.ForWhom
import org.junit.Assert.*
import org.junit.Test

class ForWhomTest {

    @Test
    fun labelForViewer_whenViewerIsOwner() {
        val partnerName = "שרה"
        assertEquals("שלי", ForWhom.ME.labelForViewer(isOwner = true, partnerName = partnerName))
        assertEquals("שרה", ForWhom.PARTNER.labelForViewer(isOwner = true, partnerName = partnerName))
        assertEquals("של שנינו", ForWhom.BOTH.labelForViewer(isOwner = true, partnerName = partnerName))
    }

    @Test
    fun labelForViewer_whenViewerIsNotOwner() {
        val partnerName = "דוד" // Owner's name
        assertEquals("דוד", ForWhom.ME.labelForViewer(isOwner = false, partnerName = partnerName))
        assertEquals("שלי", ForWhom.PARTNER.labelForViewer(isOwner = false, partnerName = partnerName))
        assertEquals("של שנינו", ForWhom.BOTH.labelForViewer(isOwner = false, partnerName = partnerName))
    }

    @Test
    fun isVisibleToViewer_cognitiveLoadReduction() {
        // When showAll is false:
        // Owner sees ME + BOTH
        assertTrue(ForWhom.ME.isVisibleToViewer(isOwner = true, showAll = false))
        assertTrue(ForWhom.BOTH.isVisibleToViewer(isOwner = true, showAll = false))
        assertFalse(ForWhom.PARTNER.isVisibleToViewer(isOwner = true, showAll = false))

        // Partner sees items designated for them (PARTNER) + BOTH
        assertTrue(ForWhom.PARTNER.isVisibleToViewer(isOwner = false, showAll = false))
        assertTrue(ForWhom.BOTH.isVisibleToViewer(isOwner = false, showAll = false))
        assertFalse(ForWhom.ME.isVisibleToViewer(isOwner = false, showAll = false))

        // When showAll is true: all items are visible
        assertTrue(ForWhom.ME.isVisibleToViewer(isOwner = true, showAll = true))
        assertTrue(ForWhom.PARTNER.isVisibleToViewer(isOwner = true, showAll = true))
        assertTrue(ForWhom.BOTH.isVisibleToViewer(isOwner = true, showAll = true))
    }
}
