package com.menash.myself.core.time

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayProviderTest {
    @Test fun todayUsesJerusalemDate() {
        val zone = ZoneId.of("Asia/Jerusalem")
        val before = LocalDate.now(zone)
        val actual = TodayProvider.today()
        val after = LocalDate.now(zone)
        // Midnight may occur between the three reads.
        assertTrue(actual == before || actual == after)
    }
}
