package com.menash.myself.core.data.mapper

import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.InboxItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class InboxMappersTest {
    @Test fun inboxToDataPreservesFieldsAndRoundTrips() {
        val item = InboxItem(
            id = "inbox-1",
            text = "Remember this",
            status = "triaged",
            triagedType = "task",
            ownerUid = "u-1",
            forWhom = ForWhom.PARTNER,
            isPrivate = false,
            createdAt = Instant.parse("2026-11-05T12:34:56.123Z"),
        )

        val data = InboxMappers.inboxToData(item, "h-1")

        assertEquals(item.id, data.id)
        assertEquals(item.text, data.text)
        assertEquals("partner", data.space)
        assertEquals(item.status, data.status)
        assertEquals(item.triagedType, data.triagedType)
        assertEquals(item.ownerUid, data.userId)
        assertEquals("h-1", data.householdId)
        assertEquals(item.createdAt, data.createdAt?.toDate()?.toInstant())
        assertEquals(item, InboxMappers.inboxToDomain(data))
    }
}
