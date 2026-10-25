package com.menash.myself.core.external.hebcal

import java.time.Instant

data class IsraeliCity(
    val geonameid: Int,
    val nameHebrew: String,
    val nameEnglish: String
) {
    companion object {
        val DEFAULT_CITIES = listOf(
            IsraeliCity(281184, "ירושלים", "Jerusalem"),
            IsraeliCity(293397, "תל אביב - יפו", "Tel Aviv"),
            IsraeliCity(294801, "חיפה", "Haifa"),
            IsraeliCity(295530, "באר שבע", "Beer Sheva"),
            IsraeliCity(294071, "נתניה", "Netanya"),
            IsraeliCity(293703, "ראשון לציון", "Rishon LeZion"),
            IsraeliCity(293918, "פתח תקווה", "Petah Tikva"),
            IsraeliCity(295629, "אשדוד", "Ashdod"),
            IsraeliCity(295277, "אילת", "Eilat"),
            IsraeliCity(294098, "נצרת", "Nazareth"),
            IsraeliCity(8198797, "מודיעין-מכבים-רעות", "Modiin"),
            IsraeliCity(293729, "רחובות", "Rehovot"),
            IsraeliCity(294421, "חולון", "Holon"),
            IsraeliCity(295514, "בני ברק", "Bnei Brak")
        )
    }
}

data class HebcalCalendarItem(
    val title: String,
    val date: String,
    val category: String,
    val hebrew: String?,
    val memo: String? = null
)

data class HebcalData(
    val cityName: String?,
    val geonameid: Int?,
    val items: List<HebcalCalendarItem>,
    val fetchedAt: Instant?,
    val isStale: Boolean = false
) {
    val currentHebrewDate: String?
        get() = items.firstOrNull { it.category == "roshchodesh" || it.category == "day" || (it.hebrew != null && it.category != "candles" && it.category != "havdalah" && it.category != "holiday") }?.hebrew
            ?: items.firstOrNull { it.hebrew != null }?.hebrew

    val upcomingShabbatCandles: String?
        get() = items.firstOrNull { it.category == "candles" }?.let { "${it.hebrew ?: "הדלקת נרות"}: ${it.title.replace("Candle lighting: ", "")}" }

    val upcomingShabbatHavdalah: String?
        get() = items.firstOrNull { it.category == "havdalah" }?.let { "${it.hebrew ?: "הבדלה"}: ${it.title.replace("Havdalah: ", "")}" }
}
