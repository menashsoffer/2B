package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.repository.SettingsRepository
import com.menash.myself.core.domain.model.LocalSettings
import com.menash.myself.core.data.legacy.dao.AppDao
import com.menash.myself.core.data.legacy.entities.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSettingsRepository(
    private val dao: AppDao
) : SettingsRepository {

    override fun getSettingsFlow(): Flow<LocalSettings?> =
        dao.getSettingsFlow().map { entity ->
            entity?.let {
                LocalSettings(
                    usdToIlsRate = it.usdToIlsRate,
                    lastBackupTimestamp = it.lastBackupTimestamp
                )
            }
        }

    override suspend fun getSettings(): LocalSettings? =
        dao.getSettings()?.let {
            LocalSettings(
                usdToIlsRate = it.usdToIlsRate,
                lastBackupTimestamp = it.lastBackupTimestamp
            )
        }

    override suspend fun saveSettings(settings: LocalSettings) {
        dao.saveSettings(
            AppSettings(
                id = 1,
                usdToIlsRate = settings.usdToIlsRate,
                lastBackupTimestamp = settings.lastBackupTimestamp
            )
        )
    }

}
