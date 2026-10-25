package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val usdToIlsRate: Double = 3.11,
    val lastBackupTimestamp: Long = 0L
)
