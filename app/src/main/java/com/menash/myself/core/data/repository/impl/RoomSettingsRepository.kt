package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.repository.SettingsRepository
import com.menash.myself.data.dao.AppDao
import com.menash.myself.data.entities.AppSettings
import kotlinx.coroutines.flow.Flow

class RoomSettingsRepository(
    private val dao: AppDao
) : SettingsRepository {
    override fun getSettingsFlow(): Flow<AppSettings?> = dao.getSettingsFlow()

    override suspend fun getSettings(): AppSettings? = dao.getSettings()

    override suspend fun saveSettings(settings: AppSettings) {
        dao.saveSettings(settings)
    }
}
