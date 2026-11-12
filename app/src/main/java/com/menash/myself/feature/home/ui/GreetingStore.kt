package com.menash.myself.feature.home.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.greetingDataStore: DataStore<Preferences> by preferencesDataStore(name = "greeting_prefs")

/** lastGreetingDate plus the first day the greeting was shown. Dates are yyyy-MM-dd. */
class GreetingStore(private val dataStore: DataStore<Preferences>) {
    private val firstDate = stringPreferencesKey("greeting_first_date")
    private val lastShown = stringPreferencesKey("lastGreetingDate")

    val record: Flow<GreetingRecord> = dataStore.data.map { prefs ->
        GreetingRecord(
            firstDate = prefs[firstDate]?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            lastShown = prefs[lastShown]?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        )
    }

    suspend fun markShown(today: LocalDate) {
        dataStore.edit { prefs ->
            if (prefs[firstDate] == null) prefs[firstDate] = today.toString()
            prefs[lastShown] = today.toString()
        }
    }
}
