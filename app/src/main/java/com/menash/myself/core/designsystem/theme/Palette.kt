package com.menash.myself.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Colour roles. Names describe the job, not the hue.
 * A new palette is another [PaletteFamily]. [ActivePalette] is the only line that selects it.
 */
data class Palette(
    val background: Color,
    val card: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentStrong: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
)

data class PaletteFamily(
    val light: Palette,
    val dark: Palette,
)

val NightBlue = PaletteFamily(
    light = Palette(
        background = Color(0xFFF3F5F8),
        card = Color(0xFFFFFFFF),
        cardBorder = Color(0xFFE1E5EC),
        textPrimary = Color(0xFF1C2230),
        textSecondary = Color(0xFF667085),
        accent = Color(0xFF3F5F9E),
        accentStrong = Color(0xFF2F4C86),
        accentSoft = Color(0xFFE3E9F4),
        onAccent = Color(0xFFFFFFFF),
        success = Color(0xFF3E6B4F),
        warning = Color(0xFF7A5414),
        danger = Color(0xFF8E3D3D),
    ),
    dark = Palette(
        background = Color(0xFF11141B),
        card = Color(0xFF1A1F29),
        cardBorder = Color(0xFF29303D),
        textPrimary = Color(0xFFE6E9EF),
        textSecondary = Color(0xFF98A1B3),
        accent = Color(0xFF8AA6DD),
        accentStrong = Color(0xFFA8BFE8),
        accentSoft = Color(0xFF1F2A3F),
        onAccent = Color(0xFF0F1626),
        success = Color(0xFFA9CDB6),
        warning = Color(0xFFE4C98A),
        danger = Color(0xFFE4B0B0),
    ),
)

val ActivePalette: PaletteFamily = NightBlue
