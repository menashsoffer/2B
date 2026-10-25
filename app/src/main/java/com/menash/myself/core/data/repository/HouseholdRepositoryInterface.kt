package com.menash.myself.core.data.repository

import com.menash.myself.data.models.Household
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface HouseholdRepositoryInterface {
    val errorFlow: StateFlow<String?>
    fun clearError()
    suspend fun getOrCreateHousehold(): Household
    fun observeHousehold(): Flow<Household?>
    suspend fun invitePartner(householdId: String, email: String)
}
