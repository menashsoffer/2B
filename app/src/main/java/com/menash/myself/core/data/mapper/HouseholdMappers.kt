package com.menash.myself.core.data.mapper

import com.menash.myself.core.domain.model.Household

object HouseholdMappers {

    fun householdToDomain(data: com.menash.myself.core.data.legacy.models.Household): Household {
        return Household(
            id = data.id,
            name = data.name,
            ownerUid = data.ownerUid,
            memberUids = data.memberUids,
            partnerEmail = data.partnerEmail,
            partnerUid = data.partnerUid
        )
    }

    fun householdToData(domain: Household): com.menash.myself.core.data.legacy.models.Household {
        return com.menash.myself.core.data.legacy.models.Household(
            id = domain.id,
            name = domain.name,
            ownerUid = domain.ownerUid,
            memberUids = domain.memberUids,
            partnerEmail = domain.partnerEmail,
            partnerUid = domain.partnerUid
        )
    }
}
