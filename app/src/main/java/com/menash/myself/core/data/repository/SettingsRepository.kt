package com.menash.myself.core.data.repository

import com.menash.myself.data.entities.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettingsFlow(): Flow<AppSettings?>
    suspend fun getSettings(): AppSettings?
    suspend fun saveSettings(settings: AppSettings)
}
