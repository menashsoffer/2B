package com.menash.myself.core.domain.model

data class Household(
    val id: String,
    val name: String,
    val ownerUid: String,
    val memberUids: List<String> = emptyList(),
    val partnerEmail: String = "",
    val partnerUid: String = ""
) {
    val isShared: Boolean
        get() = memberUids.size > 1 || partnerUid.isNotBlank()
}
