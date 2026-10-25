package com.menash.myself.core.data.repository

import android.content.Context
import com.menash.myself.core.domain.model.CalculatorSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettingsFlow(): Flow<CalculatorSettings?>
    suspend fun getSettings(): CalculatorSettings?
    suspend fun saveSettings(settings: CalculatorSettings)
    suspend fun createAndShareBackup(context: Context)
    suspend fun restoreFromJson(context: Context, jsonString: String): Boolean
}
