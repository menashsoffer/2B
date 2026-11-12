package com.menash.myself.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map

/** Chosen once, when the stored record first arrives. Later writes do not change it. */
data class GreetingDecision(
    val text: String,
    val animate: Boolean,
    val mark: Boolean,
)

@Composable
fun rememberGreetingDecision(
    record: GreetingRecord?,
    today: LocalDate,
    displayName: String?,
    reducedMotion: Boolean,
): GreetingDecision? {
    val frozen = remember(today) { mutableStateOf(record) }
    SideEffect {
        if (frozen.value == null && record != null) frozen.value = record
    }
    val loaded = frozen.value ?: return null
    return GreetingDecision(
        text = greetingLine(greetingVisit(loaded, today), displayName),
        animate = shouldAnimateGreeting(loaded, today, reducedMotion),
        mark = loaded.lastShown != today,
    )
}

/** The greeting בית renders, decided from the visit store and frozen for this calendar day. */
@Composable
fun rememberVisitGreeting(
    store: GreetingStore,
    displayName: String?,
    today: LocalDate,
    reducedMotion: Boolean,
): GreetingDecision? {
    val record by store.record.map<GreetingRecord, GreetingRecord?> { it }
        .collectAsStateWithLifecycle(initialValue = null)
    val decision = rememberGreetingDecision(record, today, displayName, reducedMotion)
    LaunchedEffect(decision) {
        val chosen = decision ?: return@LaunchedEffect
        if (!chosen.mark) return@LaunchedEffect
        if (chosen.animate) delay(1_000)
        store.markShown(today)
    }
    return decision
}
