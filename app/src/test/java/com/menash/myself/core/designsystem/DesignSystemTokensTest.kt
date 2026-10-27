package com.menash.myself.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.menash.myself.core.designsystem.component.MoneyFormatter
import com.menash.myself.core.designsystem.theme.ColorTokens
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class DesignSystemTokensTest {

    @Test
    fun moneyFormatter_wrapsInUnicodeBidiIsolation() {
        // Positive amount
        val formattedPositive = MoneyFormatter.format(1250.0)
        assertEquals("\u2066₪1,250\u2069", formattedPositive)

        // Negative amount
        val formattedNegative = MoneyFormatter.format(-1250.0)
        assertEquals("\u2066-₪1,250\u2069", formattedNegative)

        // Money object negative
        val formattedMoneyNeg = MoneyFormatter.format(Money.fromShekels(-450.5), includeAgorot = true)
        assertEquals("\u2066-₪450.50\u2069", formattedMoneyNeg)
    }

    @Test
    fun colorTokens_badges_haveSufficientContrastInBothThemes() {
        for (forWhom in ForWhom.values()) {
            // Light theme contrast
            val lightColors = ColorTokens.forWhomColors(forWhom, isDarkTheme = false)
            val lightContrast = calculateContrastRatio(lightColors.content, lightColors.background)
            assertTrue(
                "Light theme contrast for $forWhom should be >= 4.5 (WCAG AA), was $lightContrast",
                lightContrast >= 4.5
            )

            // Dark theme contrast
            val darkColors = ColorTokens.forWhomColors(forWhom, isDarkTheme = true)
            val darkContrast = calculateContrastRatio(darkColors.content, darkColors.background)
            assertTrue(
                "Dark theme contrast for $forWhom should be >= 4.5 (WCAG AA), was $darkContrast",
                darkContrast >= 4.5
            )
        }
    }

    private fun calculateContrastRatio(foreground: Color, background: Color): Double {
        val l1 = calculateLuminance(foreground)
        val l2 = calculateLuminance(background)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun calculateLuminance(color: Color): Double {
        fun channelLuminance(c: Float): Double {
            return if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val r = channelLuminance(color.red)
        val g = channelLuminance(color.green)
        val b = channelLuminance(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
