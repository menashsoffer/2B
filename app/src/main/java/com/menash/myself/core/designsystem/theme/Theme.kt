package com.menash.myself.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

val LocalAppColors = staticCompositionLocalOf { ActivePalette.light }

val MaterialTheme.appColors: Palette
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current

private fun Palette.toColorScheme(dark: Boolean): ColorScheme {
    val roles = this
    val base = if (dark) {
        darkColorScheme(
            primary = roles.accent,
            onPrimary = roles.onAccent,
            primaryContainer = roles.accentSoft,
            onPrimaryContainer = roles.accentStrong,
            secondary = roles.accentStrong,
            onSecondary = roles.onAccent,
            secondaryContainer = roles.accentSoft,
            onSecondaryContainer = roles.textPrimary,
            tertiary = roles.warning,
            onTertiary = roles.onAccent,
            background = roles.background,
            onBackground = roles.textPrimary,
            surface = roles.card,
            onSurface = roles.textPrimary,
            surfaceVariant = roles.accentSoft,
            onSurfaceVariant = roles.textSecondary,
            outline = roles.cardBorder,
            error = roles.danger,
            onError = roles.onAccent,
            errorContainer = roles.danger.copy(alpha = 0.16f),
            onErrorContainer = roles.textPrimary,
        )
    } else {
        lightColorScheme(
            primary = roles.accent,
            onPrimary = roles.onAccent,
            primaryContainer = roles.accentSoft,
            onPrimaryContainer = roles.accentStrong,
            secondary = roles.accentStrong,
            onSecondary = roles.onAccent,
            secondaryContainer = roles.accentSoft,
            onSecondaryContainer = roles.textPrimary,
            tertiary = roles.warning,
            onTertiary = roles.onAccent,
            background = roles.background,
            onBackground = roles.textPrimary,
            surface = roles.card,
            onSurface = roles.textPrimary,
            surfaceVariant = roles.accentSoft,
            onSurfaceVariant = roles.textSecondary,
            outline = roles.cardBorder,
            error = roles.danger,
            onError = roles.onAccent,
            errorContainer = roles.danger.copy(alpha = 0.12f),
            onErrorContainer = roles.textPrimary,
        )
    }
    return base.withPaletteSurfaces(roles)
}

private fun ColorScheme.withPaletteSurfaces(roles: Palette): ColorScheme = copy(
    surface = roles.card,
    surfaceContainerLowest = roles.card,
    surfaceContainerLow = roles.card,
    surfaceContainer = roles.card,
    surfaceContainerHigh = roles.card,
    surfaceContainerHighest = roles.card,
    surfaceBright = roles.card,
    surfaceDim = roles.background,
    surfaceTint = Color.Transparent,
    outlineVariant = roles.cardBorder,
    inverseSurface = roles.textPrimary,
    inverseOnSurface = roles.card,
    scrim = roles.textPrimary.copy(alpha = 0.32f),
)

private val AppMaterialShapes = Shapes(
    small = AppShapes.pill,
    medium = AppShapes.card,
    large = AppShapes.card,
    extraLarge = AppShapes.card,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) ActivePalette.dark else ActivePalette.light
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalAppColors provides palette,
    ) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(darkTheme),
            typography = AppTypography,
            shapes = AppMaterialShapes,
            content = content,
        )
    }
}
