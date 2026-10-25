package com.menash.myself.core.designsystem.tokens

import androidx.compose.ui.graphics.Color
import com.menash.myself.core.domain.model.ForWhom

data class BadgeColorScheme(
    val background: Color,
    val content: Color,
    val border: Color
)

object ColorTokens {
    // Primary brand accents
    val TealPrimary = Color(0xFF0D9488)
    val TealContainer = Color(0xFFCCFBF1)
    val TealOnContainer = Color(0xFF115E59)

    // ForWhom Badges
    val MineBackground = Color(0xFFE0F2FE)
    val MineContent = Color(0xFF0369A1)
    val MineBorder = Color(0xFFBAE6FD)

    val PartnerBackground = Color(0xFFFCE7F3)
    val PartnerContent = Color(0xFFBE185D)
    val PartnerBorder = Color(0xFFFBCFE8)

    val BothBackground = Color(0xFFFEF3C7)
    val BothContent = Color(0xFFB45309)
    val BothBorder = Color(0xFFFDE68A)

    // Financial Indicators
    val FinancialGain = Color(0xFF16A34A)
    val FinancialGainContainer = Color(0xFFDCFCE7)
    val FinancialLoss = Color(0xFFDC2626)
    val FinancialLossContainer = Color(0xFFFEE2E2)

    fun forWhomColors(forWhom: ForWhom): BadgeColorScheme {
        return when (forWhom) {
            ForWhom.ME -> BadgeColorScheme(MineBackground, MineContent, MineBorder)
            ForWhom.PARTNER -> BadgeColorScheme(PartnerBackground, PartnerContent, PartnerBorder)
            ForWhom.BOTH -> BadgeColorScheme(BothBackground, BothContent, BothBorder)
        }
    }
}
