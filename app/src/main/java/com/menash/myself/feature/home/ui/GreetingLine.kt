package com.menash.myself.feature.home.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.appColors
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp

private val UnderlineEasing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

/** Underline progress for a clock that starts at 0. The line draws from 150 ms to 750 ms. */
fun greetingUnderlineFraction(elapsedMs: Int): Float = when {
    elapsedMs <= 150 -> 0f
    elapsedMs >= 750 -> 1f
    else -> UnderlineEasing.transform((elapsedMs - 150) / 600f)
}

/** Leaf progress. The leaf sprouts from 750 ms to 1000 ms. */
fun greetingLeafFraction(elapsedMs: Int): Float = when {
    elapsedMs <= 750 -> 0f
    elapsedMs >= 1000 -> 1f
    else -> FastOutSlowInEasing.transform((elapsedMs - 750) / 250f)
}

/**
 * The greeting words are visible immediately. A mint line draws right to left, then a leaf.
 * [animate] false draws the finished line, for a later visit the same day or reduced motion.
 */
@Composable
fun GreetingLine(text: String, animate: Boolean, modifier: Modifier = Modifier) {
    val underline = remember { Animatable(if (animate) 0f else 1f) }
    val leaf = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(animate) {
        if (!animate) {
            underline.snapTo(1f)
            leaf.snapTo(1f)
            return@LaunchedEffect
        }
        underline.animateTo(1f, tween(durationMillis = 600, delayMillis = 150, easing = UnderlineEasing))
    }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        leaf.animateTo(1f, tween(durationMillis = 250, delayMillis = 750, easing = FastOutSlowInEasing))
    }
    val mint = MaterialTheme.appColors.success
    Column(modifier = modifier.semantics(mergeDescendants = true) { heading() }.testTag("home_greeting")) {
        Text(
            text = text,
            style = AppType.title,
            color = MaterialTheme.appColors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clearAndSetSemantics {},
        ) {
            val y = 4.dp.toPx()
            val path = Path().apply {
                moveTo(size.width, y)
                lineTo(0f, y)
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val revealed = Path()
            measure.getSegment(0f, measure.length * underline.value, revealed, true)
            drawPath(revealed, mint, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            val leafScale = leaf.value
            if (leafScale > 0f) {
                translate(left = 0f, top = y) {
                    drawPath(leafShape(18.dp.toPx() * leafScale), mint)
                }
            }
        }
    }
}

private fun leafShape(size: Float): Path = Path().apply {
    moveTo(0f, 0f)
    quadraticTo(size * 0.15f, -size * 0.9f, size, -size * 0.2f)
    quadraticTo(size * 0.45f, size * 0.25f, 0f, 0f)
    close()
}
