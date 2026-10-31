package com.menash.myself.core.data.repository

import android.content.Context
import com.menash.myself.core.domain.model.LocalSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettingsFlow(): Flow<LocalSettings?>
    suspend fun getSettings(): LocalSettings?
    suspend fun saveSettings(settings: LocalSettings)
    suspend fun createAndShareBackup(context: Context)
    suspend fun restoreFromJson(context: Context, jsonString: String): Boolean
}
