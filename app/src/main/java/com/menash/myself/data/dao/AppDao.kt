package com.menash.myself.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.menash.myself.data.entities.AppSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Settings
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettings(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)
}
