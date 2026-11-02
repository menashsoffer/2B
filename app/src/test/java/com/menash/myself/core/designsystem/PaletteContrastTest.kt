package com.menash.myself.core.designsystem

import androidx.compose.ui.graphics.Color
import com.menash.myself.core.designsystem.theme.ActivePalette
import com.menash.myself.core.designsystem.theme.ColorTokens
import com.menash.myself.core.designsystem.theme.Palette
import com.menash.myself.core.domain.model.ForWhom
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * UX check layer (docs/ux/UX-CHECKS.md, layer 3): every text-on-surface pair of the palette must be
 * readable (WCAG AA 4.5:1) in light and dark, so a palette change that hurts legibility fails here
 * before anyone looks at a screen. A pair that fails today is listed in [knownBelow] with its ratio;
 * that list may only shrink.
 */
class PaletteContrastTest {

    private data class TextPair(val foreground: String, val background: String)

    private val textPairs = listOf(
        TextPair("textPrimary", "background"), TextPair("textPrimary", "card"), TextPair("textPrimary", "accentSoft"),
        TextPair("textPrimary", "successSoft"), TextPair("textPrimary", "warningSoft"),
        TextPair("textSecondary", "background"), TextPair("textSecondary", "card"), TextPair("textSecondary", "accentSoft"),
        TextPair("accent", "background"), TextPair("accent", "card"),
        TextPair("accentStrong", "background"), TextPair("accentStrong", "card"), TextPair("accentStrong", "accentSoft"),
        TextPair("onAccent", "accent"), TextPair("onAccent", "accentStrong"),
        TextPair("success", "background"), TextPair("success", "card"), TextPair("success", "successSoft"),
        TextPair("warning", "background"), TextPair("warning", "card"), TextPair("warning", "warningSoft"),
        TextPair("danger", "background"), TextPair("danger", "card"),
    )

    /** "mode:foreground/background" -> ratio measured when it was listed. Fix the colour, then remove the line. */
    private val knownBelow = mapOf(
        // UX-C1: secondary text on the soft accent surface (chips, selected rows) in light mode.
        "light:textSecondary/accentSoft" to 4.08,
    )

    @Test
    fun paletteTextPairs_meetWcagAa_inLightAndDark() {
        val failures = mutableListOf<String>()
        val seenKnown = mutableSetOf<String>()
        for ((mode, palette) in listOf("light" to ActivePalette.light, "dark" to ActivePalette.dark)) {
            val roles = roles(palette)
            for (pair in textPairs) {
                val key = "$mode:${pair.foreground}/${pair.background}"
                val ratio = contrast(roles.getValue(pair.foreground), roles.getValue(pair.background))
                if (ratio >= AA) continue
                if (key in knownBelow) {
                    seenKnown += key
                    assertTrue("$key got worse: ${fmt(ratio)} < listed ${knownBelow[key]}", ratio >= knownBelow.getValue(key) - 0.01)
                } else {
                    failures += "$key = ${fmt(ratio)}:1 (needs $AA)"
                }
            }
        }
        assertTrue("Palette pairs below WCAG AA:\n${failures.joinToString("\n")}", failures.isEmpty())
        val fixed = knownBelow.keys - seenKnown
        assertTrue("These pairs now pass, remove them from knownBelow: $fixed", fixed.isEmpty())
    }

    @Test
    fun forWhomBadges_contentIsReadableOnItsBackground() {
        for (dark in listOf(false, true)) {
            for (who in ForWhom.values()) {
                val badge = ColorTokens.forWhomColors(who, dark)
                val ratio = contrast(badge.content, badge.background)
                assertTrue("badge $who dark=$dark contrast ${fmt(ratio)} < $AA", ratio >= AA)
            }
        }
    }

    private fun roles(p: Palette) = mapOf(
        "background" to p.background, "card" to p.card, "textPrimary" to p.textPrimary,
        "textSecondary" to p.textSecondary, "accent" to p.accent, "accentStrong" to p.accentStrong,
        "accentSoft" to p.accentSoft, "onAccent" to p.onAccent, "success" to p.success,
        "warning" to p.warning, "danger" to p.danger, "successSoft" to p.successSoft, "warningSoft" to p.warningSoft,
    )

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun luminance(color: Color): Double {
        fun channel(c: Float): Double = if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private fun fmt(d: Double) = "%.2f".format(d)

    private companion object {
        const val AA = 4.5
    }
}
