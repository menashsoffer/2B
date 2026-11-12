package com.menash.myself.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menash.myself.R
import java.time.LocalDate
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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
    val words = stringResource(
        if (greetingVisit(loaded, today) == GreetingVisit.First) R.string.greeting_first else R.string.greeting_return,
    )
    return GreetingDecision(
        text = greetingLine(words, displayName),
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
        withContext(NonCancellable) { store.markShown(today) }
    }
    return decision
}
