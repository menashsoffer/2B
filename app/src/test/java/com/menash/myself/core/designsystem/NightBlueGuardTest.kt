package com.menash.myself.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.menash.myself.core.designsystem.theme.ActivePalette
import com.menash.myself.core.designsystem.theme.NightBlue
import com.menash.myself.core.designsystem.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class NightBlueGuardTest {

    @Test
    fun activePalette_isChosenOnOneLine() {
        assertSame(NightBlue, ActivePalette)
        assertPalette(ActivePalette.light, lightHex)
        assertPalette(ActivePalette.dark, darkHex)
    }

    @Test
    fun statusColours_meetWcagAaOnCard() {
        assertContrastAtLeast(ActivePalette.light.success, ActivePalette.light.card, 4.5)
        assertContrastAtLeast(ActivePalette.light.warning, ActivePalette.light.card, 4.5)
        assertContrastAtLeast(ActivePalette.light.danger, ActivePalette.light.card, 4.5)
        assertContrastAtLeast(ActivePalette.dark.success, ActivePalette.dark.card, 4.5)
        assertContrastAtLeast(ActivePalette.dark.warning, ActivePalette.dark.card, 4.5)
        assertContrastAtLeast(ActivePalette.dark.danger, ActivePalette.dark.card, 4.5)
    }

    @Test
    fun noNewHardCodedColoursOutsideTheme() {
        assertAllowList(colorAllowList) { text -> "Color(0x" in text }
    }

    @Test
    fun noLegacyChromeOutsideAllowList() {
        val pattern = Regex(
            """Color\.White\b|Color\.Black\b|Color\.Gray\b|Color\.LightGray\b|TopAppBar\(|containerColor = MaterialTheme\.colorScheme\.primary\b"""
        )
        assertAllowList(legacyChromeAllowList) { text -> pattern.containsMatchIn(text) }
    }

    @Test
    fun noNewRawSpOutsideTheme() {
        val pattern = Regex("""(?<![A-Za-z0-9_])\d+(?:\.\d+)?\.sp\b""")
        assertAllowList(fontSizeAllowList) { text -> pattern.containsMatchIn(text) }
    }

    private fun assertPalette(palette: Palette, expected: Map<String, Int>) {
        val actual = mapOf(
            "background" to palette.background.toArgb(),
            "card" to palette.card.toArgb(),
            "cardBorder" to palette.cardBorder.toArgb(),
            "textPrimary" to palette.textPrimary.toArgb(),
            "textSecondary" to palette.textSecondary.toArgb(),
            "accent" to palette.accent.toArgb(),
            "accentStrong" to palette.accentStrong.toArgb(),
            "accentSoft" to palette.accentSoft.toArgb(),
            "onAccent" to palette.onAccent.toArgb(),
            "success" to palette.success.toArgb(),
            "warning" to palette.warning.toArgb(),
            "danger" to palette.danger.toArgb(),
            "successSoft" to palette.successSoft.toArgb(),
            "warningSoft" to palette.warningSoft.toArgb(),
        )
        assertEquals(expected, actual)
    }

    private fun assertContrastAtLeast(foreground: Color, background: Color, minimum: Double) {
        val ratio = contrast(foreground, background)
        assertTrue("contrast $ratio was below $minimum", ratio >= minimum)
    }

    private fun contrast(foreground: Color, background: Color): Double {
        val lighter = max(luminance(foreground), luminance(background))
        val darker = min(luminance(foreground), luminance(background))
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun luminance(color: Color): Double {
        fun channel(c: Float): Double {
            return if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private fun assertAllowList(allowList: Set<String>, matches: (String) -> Boolean) {
        val root = mainSourceRoot()
        val themePrefix = "java/com/menash/myself/core/designsystem/theme/"
        val offenders = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val rel = root.toPath().relativize(file.toPath()).toString().replace('\\', '/')
            if (rel.startsWith(themePrefix)) return@forEach
            val text = file.readText()
            if (!matches(text)) return@forEach
            seen += rel
            if (rel !in allowList) offenders += rel
        }
        val stale = allowList - seen
        assertTrue("New hard-coded usage outside the allow-list: $offenders", offenders.isEmpty())
        assertTrue("Allow-list entries with nothing left to allow: $stale", stale.isEmpty())
    }

    private fun mainSourceRoot(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".")
        repeat(6) {
            val current = dir ?: return@repeat
            val appMain = File(current, "app/src/main")
            if (appMain.isDirectory) return appMain
            val moduleMain = File(current, "src/main")
            if (File(moduleMain, "java/com/menash/myself").isDirectory) return moduleMain
            dir = current.parentFile
        }
        error("Could not find app/src/main from ${System.getProperty("user.dir")}")
    }

    private val lightHex = mapOf(
        "background" to 0xFFF3F5F8.toInt(),
        "card" to 0xFFFFFFFF.toInt(),
        "cardBorder" to 0xFFE1E5EC.toInt(),
        "textPrimary" to 0xFF1C2230.toInt(),
        "textSecondary" to 0xFF667085.toInt(),
        "accent" to 0xFF3F5F9E.toInt(),
        "accentStrong" to 0xFF2F4C86.toInt(),
        "accentSoft" to 0xFFE3E9F4.toInt(),
        "onAccent" to 0xFFFFFFFF.toInt(),
        "success" to 0xFF3E6B4F.toInt(),
        "warning" to 0xFF7A5414.toInt(),
        "danger" to 0xFF8E3D3D.toInt(),
        "successSoft" to 0xFFE2EEE6.toInt(),
        "warningSoft" to 0xFFF4EAD6.toInt(),
    )

    private val darkHex = mapOf(
        "background" to 0xFF11141B.toInt(),
        "card" to 0xFF1A1F29.toInt(),
        "cardBorder" to 0xFF29303D.toInt(),
        "textPrimary" to 0xFFE6E9EF.toInt(),
        "textSecondary" to 0xFF98A1B3.toInt(),
        "accent" to 0xFF8AA6DD.toInt(),
        "accentStrong" to 0xFFA8BFE8.toInt(),
        "accentSoft" to 0xFF1F2A3F.toInt(),
        "onAccent" to 0xFF0F1626.toInt(),
        "success" to 0xFFA9CDB6.toInt(),
        "warning" to 0xFFE4C98A.toInt(),
        "danger" to 0xFFE4B0B0.toInt(),
        "successSoft" to 0xFF1D2B23.toInt(),
        "warningSoft" to 0xFF2E2717.toInt(),
    )

    private val colorAllowList = setOf(
        "java/com/menash/myself/ui/calendar/CalendarScreen.kt",
        "java/com/menash/myself/ui/catalog/FileCatalogScreen.kt",
        "java/com/menash/myself/ui/dreams/DreamCardComponents.kt",
        "java/com/menash/myself/ui/fitness/FitnessScreen.kt",
        "java/com/menash/myself/feature/home/ui/LifeHomeCards.kt",
        "java/com/menash/myself/feature/home/ui/LifeHomeScreen.kt",
        "java/com/menash/myself/feature/home/ui/LifeHomeSections.kt",
        "java/com/menash/myself/ui/household/HouseholdCardComponents.kt",
        "java/com/menash/myself/ui/inbox/InboxTriageScreen.kt",
        "java/com/menash/myself/ui/tasks/TaskCardComponents.kt",
        "java/com/menash/myself/ui/tasks/TaskDialogs.kt",
    )

    private val legacyChromeAllowList = setOf(
        "java/com/menash/myself/core/auth/SignInScreen.kt",
        "java/com/menash/myself/ui/calendar/CalendarScreen.kt",
        "java/com/menash/myself/ui/catalog/FileCatalogScreen.kt",
        "java/com/menash/myself/ui/common/CsvMappingDialog.kt",
        "java/com/menash/myself/ui/dreams/DreamCardComponents.kt",
        "java/com/menash/myself/ui/dreams/DreamsScreen.kt",
        "java/com/menash/myself/ui/fitness/FitnessScreen.kt",
        "java/com/menash/myself/feature/home/ui/HomeHebcalCard.kt",
        "java/com/menash/myself/feature/home/ui/LifeHomeCards.kt",
        "java/com/menash/myself/feature/home/ui/LifeHomeScreen.kt",
        "java/com/menash/myself/feature/home/ui/LifeHomeSections.kt",
        "java/com/menash/myself/ui/household/HouseholdCardComponents.kt",
        "java/com/menash/myself/ui/household/HouseholdScreen.kt",
        "java/com/menash/myself/ui/inbox/InboxTriageScreen.kt",
        "java/com/menash/myself/ui/more/MoreScreen.kt",
        "java/com/menash/myself/ui/settings/CityPickerDialog.kt",
        "java/com/menash/myself/ui/settings/SettingsDialog.kt",
        "java/com/menash/myself/ui/tasks/TaskCardComponents.kt",
        "java/com/menash/myself/ui/tasks/TasksScreen.kt",
    )

    private val fontSizeAllowList = setOf(
        "java/com/menash/myself/core/auth/SignInScreen.kt",
        "java/com/menash/myself/ui/calendar/CalendarScreen.kt",
        "java/com/menash/myself/ui/catalog/FileCatalogScreen.kt",
        "java/com/menash/myself/ui/common/CsvMappingDialog.kt",
        "java/com/menash/myself/ui/dreams/DreamCardComponents.kt",
        "java/com/menash/myself/ui/dreams/DreamDialogs.kt",
        "java/com/menash/myself/ui/dreams/DreamsScreen.kt",
        "java/com/menash/myself/ui/fitness/FitnessScreen.kt",
        "java/com/menash/myself/feature/home/ui/HomeHebcalCard.kt",
        "java/com/menash/myself/feature/home/ui/LifeHomeCards.kt",
        "java/com/menash/myself/feature/home/ui/LifeHomeScreen.kt",
        "java/com/menash/myself/feature/home/ui/LifeHomeSections.kt",
        "java/com/menash/myself/ui/household/HouseholdCardComponents.kt",
        "java/com/menash/myself/ui/inbox/InboxTriageScreen.kt",
        "java/com/menash/myself/ui/more/MoreScreen.kt",
        "java/com/menash/myself/ui/settings/CityPickerDialog.kt",
        "java/com/menash/myself/ui/settings/SettingsDialog.kt",
        "java/com/menash/myself/ui/tasks/TaskCardComponents.kt",
        "java/com/menash/myself/ui/tasks/TaskDialogs.kt",
        "java/com/menash/myself/ui/tasks/TasksScreen.kt",
    )
}
