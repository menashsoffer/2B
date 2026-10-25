package com.menash.myself.core.domain.model

import java.time.Instant

data class InboxItem(
    val id: String = "",
    val text: String,
    val status: String = "inbox", // inbox, triaged
    val triagedType: String = "",
    val ownerUid: String = "",
    val forWhom: ForWhom = ForWhom.ME,
    val isPrivate: Boolean = false,
    val createdAt: Instant? = null
)
