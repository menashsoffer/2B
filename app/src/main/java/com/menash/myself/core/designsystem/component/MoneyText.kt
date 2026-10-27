package com.menash.myself.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import com.menash.myself.core.designsystem.theme.TypographyTokens
import com.menash.myself.core.designsystem.theme.tabular
import com.menash.myself.core.domain.model.Money
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Design system component for displaying currency amounts in standard Israeli New Shekel (NIS) format.
 * Relocated from Money.formatShekels to ensure pure domain models have zero presentation logic.
 * Wraps formatted currency in Unicode Bidi isolation (\u2066...\u2069) so "-₪1,250" renders correctly inside RTL layout.
 */
object MoneyFormatter {

    // Unicode Bidi Isolation characters:
    // \u2066: LEFT-TO-RIGHT ISOLATE (LRI)
    // \u2069: POP DIRECTIONAL ISOLATE (PDI)
    const val LRI = "\u2066"
    const val PDI = "\u2069"

    fun format(money: Money, includeAgorot: Boolean = false): String {
        return formatShekels(money.shekels, money.agorot < 0, includeAgorot)
    }

    fun format(shekels: Double, includeAgorot: Boolean = false): String {
        return formatShekels(shekels, shekels < 0, includeAgorot)
    }

    private fun formatShekels(shekels: Double, isNegative: Boolean, includeAgorot: Boolean): String {
        val absShekels = abs(shekels)
        val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
            if (includeAgorot) {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            } else {
                minimumFractionDigits = 0
                maximumFractionDigits = 0
            }
        }
        val formattedNumber = fmt.format(absShekels)
        val signPrefixed = if (isNegative) "-₪$formattedNumber" else "₪$formattedNumber"
        return "$LRI$signPrefixed$PDI"
    }
}

@Composable
fun MoneyText(
    money: Money,
    modifier: Modifier = Modifier,
    includeAgorot: Boolean = false,
    color: Color = Color.Unspecified,
    style: TextStyle = TypographyTokens.MoneyMedium,
    testTag: String = "money_text"
) {
    Text(
        text = MoneyFormatter.format(money, includeAgorot),
        color = color,
        style = style.tabular(),
        modifier = modifier.testTag(testTag)
    )
}

@Composable
fun MoneyText(
    shekels: Double,
    modifier: Modifier = Modifier,
    includeAgorot: Boolean = false,
    color: Color = Color.Unspecified,
    style: TextStyle = TypographyTokens.MoneyMedium,
    testTag: String = "money_text"
) {
    Text(
        text = MoneyFormatter.format(shekels, includeAgorot),
        color = color,
        style = style.tabular(),
        modifier = modifier.testTag(testTag)
    )
}
