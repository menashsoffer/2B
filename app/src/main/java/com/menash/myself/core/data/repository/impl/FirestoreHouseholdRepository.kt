package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.repository.HouseholdRepositoryInterface
import com.menash.myself.data.firestore.HouseholdRepository
import com.menash.myself.data.models.Household
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class FirestoreHouseholdRepository(private val householdRepo: HouseholdRepository) : HouseholdRepositoryInterface {
    override val errorFlow: StateFlow<String?> = householdRepo.errorFlow

    override fun clearError() = householdRepo.clearError()

    override suspend fun getOrCreateHousehold(): Household = householdRepo.getOrCreateHousehold()

    override fun observeHousehold(): Flow<Household?> = householdRepo.observeHousehold()

    override suspend fun invitePartner(householdId: String, email: String) =
        householdRepo.invitePartner(householdId, email)
}
