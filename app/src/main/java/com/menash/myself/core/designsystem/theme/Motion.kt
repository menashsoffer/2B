package com.menash.myself.core.designsystem.theme

import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** Shared, restrained motion. Compose applies the system's animation duration scale. */
object Motion {
    const val short = 150
    const val medium = 220
    val easing = FastOutSlowInEasing

    @Composable
    fun duration(millis: Int): Int {
        val resolver = LocalContext.current.contentResolver
        val scale = runCatching {
            Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        return if (scale == 0f) 0 else millis
    }

    fun fadeIn(duration: Int): EnterTransition = fadeIn(tween(duration, easing = easing))
    fun fadeOut(duration: Int): ExitTransition = fadeOut(tween(duration, easing = easing))
    fun expand(duration: Int): EnterTransition = expandVertically(tween(duration, easing = easing)) + fadeIn(duration)
    fun shrink(duration: Int): ExitTransition = shrinkVertically(tween(duration, easing = easing)) + fadeOut(duration)

    @Composable
    fun slideIn(duration: Int): EnterTransition {
        val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
        return slideInHorizontally(tween(duration, easing = easing)) { width -> direction * width } + fadeIn(duration)
    }

    @Composable
    fun slideOut(duration: Int): ExitTransition {
        val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
        return slideOutHorizontally(tween(duration, easing = easing)) { width -> direction * width } + fadeOut(duration)
    }
}
