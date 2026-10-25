package com.menash.myself.core.data.repository

import com.menash.myself.core.domain.model.Household
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface HouseholdRepositoryInterface {
    val errorFlow: StateFlow<String?>
        get() = MutableStateFlow(null)

    fun clearError() {}

    suspend fun getOrCreateHousehold(): Household =
        Household(id = ensureHouseholdExists(), name = "", ownerUid = "")

    fun observeHousehold(): Flow<Household?>

    fun observeMyHousehold(): Flow<Household?> = observeHousehold()

    suspend fun ensureHouseholdExists(): String = getOrCreateHousehold().id

    suspend fun invitePartner(householdId: String, email: String)
}
