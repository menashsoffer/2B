package com.menash.myself.core.data.repository.impl

import android.content.Context
import com.menash.myself.core.data.repository.SettingsRepository
import com.menash.myself.core.domain.model.CalculatorSettings
import com.menash.myself.data.dao.AppDao
import com.menash.myself.data.entities.AppSettings
import com.menash.myself.data.io.BackupRestoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSettingsRepository(
    private val dao: AppDao
) : SettingsRepository {

    override fun getSettingsFlow(): Flow<CalculatorSettings?> =
        dao.getSettingsFlow().map { entity ->
            entity?.let {
                CalculatorSettings(
                    usdToIlsRate = it.usdToIlsRate,
                    lastBackupTimestamp = it.lastBackupTimestamp
                )
            }
        }

    override suspend fun getSettings(): CalculatorSettings? =
        dao.getSettings()?.let {
            CalculatorSettings(
                usdToIlsRate = it.usdToIlsRate,
                lastBackupTimestamp = it.lastBackupTimestamp
            )
        }

    override suspend fun saveSettings(settings: CalculatorSettings) {
        dao.saveSettings(
            AppSettings(
                id = 1,
                usdToIlsRate = settings.usdToIlsRate,
                lastBackupTimestamp = settings.lastBackupTimestamp
            )
        )
    }

    override suspend fun createAndShareBackup(context: Context) {
        BackupRestoreManager.createAndShareBackup(context)
    }

    override suspend fun restoreFromJson(context: Context, jsonString: String): Boolean {
        return BackupRestoreManager.restoreFromJson(context, jsonString)
    }
}
