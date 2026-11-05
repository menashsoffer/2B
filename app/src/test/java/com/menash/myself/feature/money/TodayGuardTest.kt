package com.menash.myself.feature.money

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TodayGuardTest {
    @Test fun moneyCodeUsesInjectedToday() {
        val root = File("src/main/java/com/menash/myself")
        val forbidden = Regex("\\b(?:LocalDate|YearMonth)\\s*\\.\\s*now\\s*\\(")
        val violations = listOf("feature/money").flatMap { path ->
            File(root, path).walkTopDown().filter { it.isFile && it.extension == "kt" }
                .filter { forbidden.containsMatchIn(it.readText()) }.map { it.path }.toList()
        }
        assertTrue("Use the injected today provider: $violations", violations.isEmpty())
    }
}
