package com.menash.myself.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import java.time.LocalDate

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
