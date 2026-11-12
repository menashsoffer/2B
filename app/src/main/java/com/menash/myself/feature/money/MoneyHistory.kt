package com.menash.myself.feature.money

/** A money screen on the walk-order stack. Root is an empty stack. */
enum class MoneyPlace {
    Goals,
    Tithe,
    Car,
    Pension,
    Subscriptions,
    Period,
    Close,
    Summary,
    Transactions,
    Trend,
    Import,
}

/** One opened money screen, plus the arguments needed to open it again after process death. */
data class MoneyFrame(
    val place: MoneyPlace,
    val month: String? = null,
    val filter: String? = null,
    val categoryId: String? = null,
) {
    fun encode(): String = listOf(place.name, month.orEmpty(), filter.orEmpty(), categoryId.orEmpty()).joinToString("|")

    companion object {
        fun decode(raw: String): MoneyFrame? {
            val parts = raw.split("|")
            if (parts.size != 4) return null
            val place = runCatching { MoneyPlace.valueOf(parts[0]) }.getOrNull() ?: return null
            return MoneyFrame(
                place = place,
                month = parts[1].ifEmpty { null },
                filter = parts[2].ifEmpty { null },
                categoryId = parts[3].ifEmpty { null },
            )
        }
    }
}

/**
 * Keeps screens in the order they were opened.
 * Screens that are no longer open drop out. Newly opened screens go on top.
 * A shared file closes the open screens first, so Back from import returns to the money root.
 */
fun syncMoneyHistory(history: List<MoneyFrame>, open: List<MoneyFrame>): List<MoneyFrame> {
    val byPlace = open.associateBy { it.place }
    val kept = history.mapNotNull { frame -> byPlace[frame.place] }
    val seen = kept.map { it.place }.toSet()
    return kept + open.filter { it.place !in seen }
}

fun encodeMoneyHistory(frames: List<MoneyFrame>): String = frames.joinToString(";") { it.encode() }

fun decodeMoneyHistory(raw: String): List<MoneyFrame> =
    if (raw.isEmpty()) emptyList() else raw.split(";").mapNotNull { MoneyFrame.decode(it) }

/** After process death the open flags are empty, so every saved frame is replayed. */
fun framesToReplay(history: List<MoneyFrame>, openPlaces: Set<MoneyPlace>): List<MoneyFrame> =
    history.filter { it.place !in openPlaces }
