package com.menash.myself.core.external.boi

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

val Context.boiDataStore: DataStore<Preferences> by preferencesDataStore(name = "boi_rate_prefs")

/**
 * DataStore cache for Bank of Israel USD/ILS representative rate and manual override.
 */
class BoiRateStore(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val KEY_RATE = doublePreferencesKey("boi_rate_val")
        private val KEY_RATE_DATE = stringPreferencesKey("boi_rate_date")
        private val KEY_FETCHED_AT = longPreferencesKey("boi_fetched_at")
        private val KEY_MANUAL_RATE = doublePreferencesKey("boi_manual_rate")
        private val KEY_IS_MANUAL_OVERRIDE = booleanPreferencesKey("boi_is_manual_override")
    }

    val rateFlow: Flow<BoiRate?> = dataStore.data.map { prefs ->
        val isManual = prefs[KEY_IS_MANUAL_OVERRIDE] ?: false
        val manualRate = prefs[KEY_MANUAL_RATE]
        val cachedRate = prefs[KEY_RATE]
        val dateStr = prefs[KEY_RATE_DATE]
        val fetchedEpoch = prefs[KEY_FETCHED_AT]

        val rateDate = dateStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val fetchedAt = fetchedEpoch?.let { Instant.ofEpochMilli(it) }

        val hasOfficial = cachedRate != null && cachedRate > 0.0 && rateDate != null && fetchedAt != null

        if (hasOfficial || (manualRate != null && manualRate > 0.0)) {
            BoiRate(
                officialRate = if (hasOfficial) cachedRate else null,
                officialRateDate = if (hasOfficial) rateDate else null,
                officialFetchedAt = if (hasOfficial) fetchedAt else null,
                manualRate = manualRate,
                isManualOverride = isManual
            )
        } else {
            null
        }
    }

    suspend fun saveAutoRate(rate: BoiRate) {
        dataStore.edit { prefs ->
            if (rate.officialRate != null) {
                prefs[KEY_RATE] = rate.officialRate
            }
            if (rate.officialRateDate != null) {
                prefs[KEY_RATE_DATE] = rate.officialRateDate.toString()
            }
            if (rate.officialFetchedAt != null) {
                prefs[KEY_FETCHED_AT] = rate.officialFetchedAt.toEpochMilli()
            }
        }
    }

    suspend fun setManualOverride(rate: Double?) {
        dataStore.edit { prefs ->
            if (rate != null && rate > 0.0) {
                prefs[KEY_MANUAL_RATE] = rate
                prefs[KEY_IS_MANUAL_OVERRIDE] = true
            } else {
                prefs[KEY_IS_MANUAL_OVERRIDE] = false
            }
        }
    }

    suspend fun clearManualOverride() {
        dataStore.edit { prefs ->
            prefs[KEY_IS_MANUAL_OVERRIDE] = false
        }
    }
}
