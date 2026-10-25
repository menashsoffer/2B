package com.menash.myself.core.external.hebcal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * REST Client for Hebcal API.
 * Endpoint: https://www.hebcal.com/hebcal?v=1&cfg=json&maj=on&min=on&mod=on&nx=on&year=now&month=x&ss=on&mf=on&c=on&geo=geoname&geonameid={geonameid}
 */
class HebcalClient {

    suspend fun fetchCalendarData(geonameid: Int, cityName: String): Result<HebcalData> = withContext(Dispatchers.IO) {
        runCatching {
            val urlStr = "https://www.hebcal.com/hebcal?v=1&cfg=json&maj=on&min=on&mod=on&nx=on&year=now&month=x&ss=on&mf=on&c=on&geo=geoname&geonameid=$geonameid"
            val jsonStr = fetchUrl(urlStr)
            parseHebcalJson(jsonStr, geonameid, cityName)
        }
    }

    fun parseHebcalJson(jsonStr: String, geonameid: Int?, cityName: String?, nowInstant: Instant = Instant.now()): HebcalData {
        val root = JSONObject(jsonStr)
        val itemsArray = root.optJSONArray("items") ?: return HebcalData(cityName, geonameid, emptyList(), nowInstant)

        val items = mutableListOf<HebcalCalendarItem>()
        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.getJSONObject(i)
            items.add(
                HebcalCalendarItem(
                    title = obj.optString("title"),
                    date = obj.optString("date"),
                    category = obj.optString("category"),
                    hebrew = obj.optString("hebrew").takeIf { it.isNotBlank() },
                    memo = obj.optString("memo").takeIf { it.isNotBlank() }
                )
            )
        }

        return HebcalData(
            cityName = cityName,
            geonameid = geonameid,
            items = items,
            fetchedAt = nowInstant,
            isStale = false
        )
    }

    private fun fetchUrl(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("User-Agent", "MyselfApp/1.0 (Android)")
        }

        return conn.inputStream.bufferedReader().use { it.readText() }
    }
}
