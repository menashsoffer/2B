package com.menash.myself.core.domain.model

/**
 * The settings row kept on the device (Room table app_settings) and in the JSON backup.
 * The money pillar reads the USD rate from the Bank of Israel store, not from here.
 */
data class LocalSettings(
    val usdToIlsRate: Double = 3.11,
    val lastBackupTimestamp: Long = 0L
)
