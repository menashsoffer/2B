package com.menash.myself.ui.money

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Keeps the last open state while AnimatedContent renders an outgoing destination. */
@Composable
internal fun <T> rememberShownState(current: T, isOpen: Boolean): T {
    var shown by remember { mutableStateOf(current) }
    if (isOpen) shown = current
    return shown
}
