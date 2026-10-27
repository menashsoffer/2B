package com.menash.myself.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.menash.myself.core.domain.model.ForWhom

data class BadgeColorScheme(
    val background: Color,
    val content: Color,
    val border: Color
)

object ColorTokens {
    val TealPrimary = Color(0xFF0D9488)
    val TealContainer = Color(0xFFCCFBF1)
    val TealOnContainer = Color(0xFF115E59)

    val MineBackgroundLight = Color(0xFFE0F2FE)
    val MineContentLight = Color(0xFF0369A1)
    val MineBorderLight = Color(0xFFBAE6FD)

    val PartnerBackgroundLight = Color(0xFFFCE7F3)
    val PartnerContentLight = Color(0xFFBE185D)
    val PartnerBorderLight = Color(0xFFFBCFE8)

    val BothBackgroundLight = Color(0xFFFEF3C7)
    val BothContentLight = Color(0xFF92400E)
    val BothBorderLight = Color(0xFFFDE68A)

    val MineBackgroundDark = Color(0xFF082F49)
    val MineContentDark = Color(0xFF7DD3FC)
    val MineBorderDark = Color(0xFF0369A1)

    val PartnerBackgroundDark = Color(0xFF4C0519)
    val PartnerContentDark = Color(0xFFF472B6)
    val PartnerBorderDark = Color(0xFFBE185D)

    val BothBackgroundDark = Color(0xFF451A03)
    val BothContentDark = Color(0xFFFCD34D)
    val BothBorderDark = Color(0xFFB45309)

    val FinancialGain = Color(0xFF16A34A)
    val FinancialGainContainer = Color(0xFFDCFCE7)
    val FinancialLoss = Color(0xFFDC2626)
    val FinancialLossContainer = Color(0xFFFEE2E2)

    fun forWhomColors(forWhom: ForWhom, isDarkTheme: Boolean = false): BadgeColorScheme {
        return if (isDarkTheme) {
            when (forWhom) {
                ForWhom.ME -> BadgeColorScheme(MineBackgroundDark, MineContentDark, MineBorderDark)
                ForWhom.PARTNER -> BadgeColorScheme(PartnerBackgroundDark, PartnerContentDark, PartnerBorderDark)
                ForWhom.BOTH -> BadgeColorScheme(BothBackgroundDark, BothContentDark, BothBorderDark)
            }
        } else {
            when (forWhom) {
                ForWhom.ME -> BadgeColorScheme(MineBackgroundLight, MineContentLight, MineBorderLight)
                ForWhom.PARTNER -> BadgeColorScheme(PartnerBackgroundLight, PartnerContentLight, PartnerBorderLight)
                ForWhom.BOTH -> BadgeColorScheme(BothBackgroundLight, BothContentLight, BothBorderLight)
            }
        }
    }
}
