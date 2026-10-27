package com.menash.myself.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.menash.myself.core.designsystem.theme.NightBlue
import com.menash.myself.core.designsystem.theme.Palette
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

/**
 * WCAG AA for the R2 components, in light and dark.
 * Colours come from [NightBlue]. There is no allow-list: a pair under its threshold fails.
 *
 * Normal text is 4.5:1. [com.menash.myself.core.designsystem.theme.AppType.number] (40sp)
 * and UI glyphs are 3:1. Where the spec table lists 4.5 for a pair that is also large text,
 * 4.5 is used (stricter).
 *
 * Hero label (and the message when a value is showing) is onAccent at alpha 0.82 composited
 * over accent. Home's hero has no value, so its message is opaque onAccent.
 */
class NewComponentsContrastTest {

    @Test
    fun textAndIconPairs_meetWcagAa() {
        val failures = mutableListOf<String>()
        cases.forEach { case ->
            themes.forEach { (themeName, palette) ->
                val ratio = contrastRatio(case.foreground(palette), case.background(palette))
                if (ratio < case.minimum) {
                    failures += "$themeName ${case.component} ${case.pair} $ratio < ${case.minimum}"
                }
            }
        }
        assertTrue(failures.joinToString(separator = "\n"), failures.isEmpty())
    }

    private data class ContrastCase(
        val component: String,
        val pair: String,
        val minimum: Double,
        val foreground: (Palette) -> Color,
        val background: (Palette) -> Color,
    )

    private val themes = listOf(
        "light" to NightBlue.light,
        "dark" to NightBlue.dark,
    )

    private val cases = listOf(
        case("Header", "greeting textPrimary on background", 4.5, { it.textPrimary }, { it.background }),
        case("Header", "date textSecondary on background", 4.5, { it.textSecondary }, { it.background }),
        case("ProfileAvatar", "letter and icon accentStrong on accentSoft", 4.5, { it.accentStrong }, { it.accentSoft }),
        case("MetricCard", "label and detail textSecondary on card", 4.5, { it.textSecondary }, { it.card }),
        case("MetricCard", "value textPrimary on card", 4.5, { it.textPrimary }, { it.card }),
        case("HeroCard", "value onAccent on accent", 3.0, { it.onAccent }, { it.accent }),
        case("HeroCard", "message onAccent on accent", 4.5, { it.onAccent }, { it.accent }),
        case(
            "HeroCard",
            "label and secondary message onAccent alpha 0.82 on accent",
            4.5,
            { compositeOver(it.onAccent, it.accent, 0.82f) },
            { it.accent },
        ),
        case("FloatingNavBar", "active label accentStrong on card", 4.5, { it.accentStrong }, { it.card }),
        case("FloatingNavBar", "active icon accentStrong on accentSoft", 3.0, { it.accentStrong }, { it.accentSoft }),
        case("FloatingNavBar", "inactive label and icon textSecondary on card", 4.5, { it.textSecondary }, { it.card }),
        case("PillTabs", "selected accentStrong on accentSoft", 4.5, { it.accentStrong }, { it.accentSoft }),
        case("PillTabs", "unselected textSecondary on card", 4.5, { it.textSecondary }, { it.card }),
        case("Chip", "default text and icon accentStrong on accentSoft", 4.5, { it.accentStrong }, { it.accentSoft }),
        case("Chip", "success text and icon success on successSoft", 4.5, { it.success }, { it.successSoft }),
        case("Chip", "warning text and icon warning on warningSoft", 4.5, { it.warning }, { it.warningSoft }),
        case("PrimaryButton", "label onAccent on accent", 4.5, { it.onAccent }, { it.accent }),
        case("EmptyState", "description textSecondary on background", 4.5, { it.textSecondary }, { it.background }),
        case("AccountSheet", "sign-out text and icon danger on card", 4.5, { it.danger }, { it.card }),
        case("AccountSheet", "settings icon accent on card", 3.0, { it.accent }, { it.card }),
    )

    private fun case(
        component: String,
        pair: String,
        minimum: Double,
        foreground: (Palette) -> Color,
        background: (Palette) -> Color,
    ): ContrastCase = ContrastCase(component, pair, minimum, foreground, background)
}

/**
 * Src-over composite, rounded to 8-bit channels the way the spec script mixes alpha.
 * Inputs are palette colours, not literals.
 */
internal fun compositeOver(foreground: Color, background: Color, alpha: Float): Color {
    fun mix(shift: Int): Int {
        val fg = channel(foreground, shift)
        val bg = channel(background, shift)
        return round(alpha * fg + (1f - alpha) * bg).toInt()
    }
    return Color(red = mix(16), green = mix(8), blue = mix(0))
}

internal fun contrastRatio(foreground: Color, background: Color): Double {
    val lighter = max(relativeLuminance(foreground), relativeLuminance(background))
    val darker = min(relativeLuminance(foreground), relativeLuminance(background))
    return (lighter + 0.05) / (darker + 0.05)
}

private fun channel(color: Color, shift: Int): Int = (color.toArgb() ushr shift) and 0xFF

private fun relativeLuminance(color: Color): Double {
    fun encode(component: Int): Double {
        val value = component / 255.0
        return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * encode(channel(color, 16)) +
        0.7152 * encode(channel(color, 8)) +
        0.0722 * encode(channel(color, 0))
}
