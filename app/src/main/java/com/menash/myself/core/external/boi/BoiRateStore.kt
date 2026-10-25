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

        if (cachedRate != null && cachedRate > 0.0 && rateDate != null && fetchedAt != null) {
            val isStale = BoiStalenessPolicy.isStale(rateDate, fetchedAt, Instant.now())
            BoiRate(
                officialRate = cachedRate,
                officialRateDate = rateDate,
                officialFetchedAt = fetchedAt,
                manualRate = manualRate,
                isManualOverride = isManual,
                isStale = isStale
            )
        } else if (manualRate != null && manualRate > 0.0) {
            BoiRate(
                officialRate = manualRate,
                officialRateDate = rateDate ?: LocalDate.now(BoiStalenessPolicy.ISRAEL_ZONE),
                officialFetchedAt = fetchedAt ?: Instant.now(),
                manualRate = manualRate,
                isManualOverride = isManual,
                isStale = false
            )
        } else {
            null
        }
    }

    suspend fun saveAutoRate(rate: BoiRate) {
        dataStore.edit { prefs ->
            prefs[KEY_RATE] = rate.officialRate
            prefs[KEY_RATE_DATE] = rate.officialRateDate.toString()
            prefs[KEY_FETCHED_AT] = rate.officialFetchedAt.toEpochMilli()
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
