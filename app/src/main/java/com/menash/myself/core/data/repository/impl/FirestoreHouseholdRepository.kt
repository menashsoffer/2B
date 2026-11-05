package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.mapper.HouseholdMappers
import com.menash.myself.core.data.repository.HouseholdRepositoryInterface
import com.menash.myself.core.data.session.HouseholdSession
import com.menash.myself.core.domain.model.Household
import com.menash.myself.core.data.legacy.firestore.HouseholdRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class FirestoreHouseholdRepository(
    private val householdRepo: HouseholdRepository,
    private val householdSession: HouseholdSession
) : HouseholdRepositoryInterface {

    override val errorFlow: StateFlow<String?> = householdRepo.errorFlow

    override fun clearError() = householdRepo.clearError()

    override suspend fun getOrCreateHousehold(): Household {
        val dataHousehold = householdRepo.getOrCreateHousehold()
        householdSession.setHouseholdId(dataHousehold.id)
        return HouseholdMappers.householdToDomain(dataHousehold)
    }

    override fun observeHousehold(): Flow<Household?> {
        return householdRepo.observeHousehold()
            .onEach { data ->
                householdSession.setHouseholdId(data?.id)
            }
            .map { it?.let { HouseholdMappers.householdToDomain(it) } }
    }

    override suspend fun invitePartner(householdId: String, email: String) {
        householdRepo.invitePartner(householdId, email)
    }
}
