package com.menash.myself.core.designsystem

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.menash.myself.core.designsystem.component.MetricValueStyle
import com.menash.myself.core.designsystem.component.metricValueTextStyle
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.tabular
import org.junit.Assert.assertEquals
import org.junit.Test

class AppTypeNumberCompactTest {

    @Test
    fun numberCompact_is28SpTabularAtTheNumberLineHeightRatio() {
        val number = AppType.number
        val compact = AppType.numberCompact
        assertEquals(28.sp, compact.fontSize)
        assertEquals(33.6.sp, compact.lineHeight)
        assertEquals("tnum", compact.fontFeatureSettings)
        assertEquals(number.fontFeatureSettings, compact.fontFeatureSettings)
        assertEquals(FontWeight.Medium, compact.fontWeight)
        assertEquals(number.fontFamily, compact.fontFamily)
        val numberRatio = number.lineHeight.value / number.fontSize.value
        val compactRatio = compact.lineHeight.value / compact.fontSize.value
        assertEquals(numberRatio, compactRatio, 0.001f)
    }

    @Test
    fun metricValueStyle_compactUsesNumberCompact_mediumStaysTitle() {
        assertEquals(AppType.number, MetricValueStyle.Large.metricValueTextStyle())
        assertEquals(AppType.numberCompact, MetricValueStyle.Compact.metricValueTextStyle())
        assertEquals(AppType.title.tabular(), MetricValueStyle.Medium.metricValueTextStyle())
    }
}
