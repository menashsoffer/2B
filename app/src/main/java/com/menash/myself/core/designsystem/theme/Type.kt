package com.menash.myself.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.menash.myself.R

val Rubik = FontFamily(
    Font(R.font.rubik, FontWeight.Normal),
    Font(R.font.rubik, FontWeight.Medium),
    Font(R.font.rubik, FontWeight.SemiBold),
    Font(R.font.rubik, FontWeight.Bold),
)

/**
 * Text styles for new screens. [number] and [numberCompact] are card figures with tabular figures.
 * [numberCompact] keeps [number]'s line-height ratio (48/40).
 */
object AppType {
    val number = TextStyle(
        fontFamily = Rubik,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        fontFeatureSettings = "tnum",
    )

    /** Half-width metric figure. 28sp, same 1.2 line-height ratio as [number]. */
    val numberCompact = TextStyle(
        fontFamily = Rubik,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 33.6.sp,
        fontFeatureSettings = "tnum",
    )
    val title = TextStyle(
        fontFamily = Rubik,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    )
    val body = TextStyle(
        fontFamily = Rubik,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )
    val caption = TextStyle(
        fontFamily = Rubik,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
}

fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum", fontFamily = Rubik)

private fun TextStyle.asRubik(): TextStyle = copy(fontFamily = Rubik)

private val materialDefaults = Typography()

/** Material styles keep their previous sizes. The family is Rubik so existing text does not reflow. */
val AppTypography = materialDefaults.copy(
    displayLarge = materialDefaults.displayLarge.asRubik(),
    displayMedium = materialDefaults.displayMedium.asRubik(),
    displaySmall = materialDefaults.displaySmall.asRubik(),
    headlineLarge = materialDefaults.headlineLarge.asRubik(),
    headlineMedium = materialDefaults.headlineMedium.asRubik(),
    headlineSmall = materialDefaults.headlineSmall.asRubik(),
    titleLarge = materialDefaults.titleLarge.asRubik(),
    titleMedium = materialDefaults.titleMedium.asRubik(),
    titleSmall = materialDefaults.titleSmall.asRubik(),
    bodyLarge = materialDefaults.bodyLarge.asRubik(),
    bodyMedium = materialDefaults.bodyMedium.asRubik(),
    bodySmall = materialDefaults.bodySmall.asRubik(),
    labelLarge = materialDefaults.labelLarge.asRubik(),
    labelMedium = materialDefaults.labelMedium.asRubik(),
    labelSmall = materialDefaults.labelSmall.asRubik(),
)

/**
 * Previous component styles, same sizes as before, now set in Rubik.
 * New screens use [AppType] only.
 */
object TypographyTokens {
    val HeadlineLarge = TextStyle(
        fontFamily = Rubik,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    )
    val HeadlineMedium = TextStyle(
        fontFamily = Rubik,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    )
    val TitleMedium = TextStyle(
        fontFamily = Rubik,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    )
    val BodyMedium = TextStyle(
        fontFamily = Rubik,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
    val BodySmall = TextStyle(
        fontFamily = Rubik,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
    val LabelMedium = TextStyle(
        fontFamily = Rubik,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
    val MoneyLarge = AppType.number
    val MoneyMedium = TextStyle(
        fontFamily = Rubik,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontFeatureSettings = "tnum",
    )
}
