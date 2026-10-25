package com.menash.myself.core.external.hebcal

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.time.Instant
import java.time.temporal.ChronoUnit

val Context.hebcalDataStore: DataStore<Preferences> by preferencesDataStore(name = "hebcal_prefs")

/**
 * DataStore cache for Hebcal city selection and weekly calendar items.
 */
class HebcalStore(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val KEY_GEONAME_ID = intPreferencesKey("hebcal_geoname_id")
        private val KEY_CITY_NAME = stringPreferencesKey("hebcal_city_name")
        private val KEY_CACHED_JSON = stringPreferencesKey("hebcal_cached_json")
        private val KEY_FETCHED_AT = longPreferencesKey("hebcal_fetched_at")
    }

    val hebcalDataFlow: Flow<HebcalData?> = dataStore.data.map { prefs ->
        val geonameid = prefs[KEY_GEONAME_ID]
        val cityName = prefs[KEY_CITY_NAME]
        val cachedJson = prefs[KEY_CACHED_JSON]
        val fetchedEpoch = prefs[KEY_FETCHED_AT]

        if (geonameid == null || cityName == null) {
            null
        } else if (cachedJson != null && fetchedEpoch != null) {
            val fetchedAt = Instant.ofEpochMilli(fetchedEpoch)
            val isStale = ChronoUnit.DAYS.between(fetchedAt, Instant.now()) >= 7
            val items = parseItemsJson(cachedJson)
            HebcalData(
                cityName = cityName,
                geonameid = geonameid,
                items = items,
                fetchedAt = fetchedAt,
                isStale = isStale
            )
        } else {
            HebcalData(
                cityName = cityName,
                geonameid = geonameid,
                items = emptyList(),
                fetchedAt = null,
                isStale = true
            )
        }
    }

    val selectedCityFlow: Flow<IsraeliCity?> = dataStore.data.map { prefs ->
        val geonameid = prefs[KEY_GEONAME_ID]
        val cityName = prefs[KEY_CITY_NAME]
        if (geonameid != null && cityName != null) {
            IsraeliCity(geonameid, cityName, "")
        } else {
            null
        }
    }

    suspend fun setSelectedCity(city: IsraeliCity) {
        dataStore.edit { prefs ->
            prefs[KEY_GEONAME_ID] = city.geonameid
            prefs[KEY_CITY_NAME] = city.nameHebrew
            // Reset cached json to force refresh for new city
            prefs.remove(KEY_CACHED_JSON)
            prefs.remove(KEY_FETCHED_AT)
        }
    }

    suspend fun saveCalendarData(data: HebcalData) {
        dataStore.edit { prefs ->
            if (data.geonameid != null) prefs[KEY_GEONAME_ID] = data.geonameid
            if (data.cityName != null) prefs[KEY_CITY_NAME] = data.cityName
            prefs[KEY_CACHED_JSON] = encodeItemsJson(data.items)
            prefs[KEY_FETCHED_AT] = (data.fetchedAt ?: Instant.now()).toEpochMilli()
        }
    }

    private fun encodeItemsJson(items: List<HebcalCalendarItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            val obj = org.json.JSONObject().apply {
                put("title", item.title)
                put("date", item.date)
                put("category", item.category)
                put("hebrew", item.hebrew ?: "")
                put("memo", item.memo ?: "")
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseItemsJson(jsonStr: String): List<HebcalCalendarItem> {
        val list = mutableListOf<HebcalCalendarItem>()
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                HebcalCalendarItem(
                    title = obj.optString("title"),
                    date = obj.optString("date"),
                    category = obj.optString("category"),
                    hebrew = obj.optString("hebrew").takeIf { it.isNotBlank() },
                    memo = obj.optString("memo").takeIf { it.isNotBlank() }
                )
            )
        }
        return list
    }
}
