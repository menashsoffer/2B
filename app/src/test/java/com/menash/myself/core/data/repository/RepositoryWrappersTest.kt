package com.menash.myself.core.data.repository

import com.menash.myself.core.data.repository.impl.RoomSettingsRepository
import com.menash.myself.core.domain.model.LocalSettings
import com.menash.myself.data.dao.AppDao
import com.menash.myself.data.entities.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The Room settings wrapper delegates to the DAO and maps the row to the domain model. */
class RepositoryWrappersTest {

    private val fakeDao = object : AppDao {
        var row: AppSettings? = null
        override fun getSettingsFlow(): Flow<AppSettings?> = flowOf(row)
        override suspend fun getSettings(): AppSettings? = row
        override suspend fun saveSettings(settings: AppSettings) {
            row = settings
        }
    }

    @Test
    fun settingsRepository_delegatesToDaoWithDomainModel() = runBlocking {
        val repo = RoomSettingsRepository(fakeDao)
        assertNull(repo.getSettings())

        repo.saveSettings(LocalSettings(usdToIlsRate = 3.7, lastBackupTimestamp = 42L))

        assertEquals(1, fakeDao.row?.id)
        assertEquals(LocalSettings(3.7, 42L), repo.getSettings())
        assertEquals(LocalSettings(3.7, 42L), repo.getSettingsFlow().first())
    }
}
