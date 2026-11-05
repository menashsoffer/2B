package com.menash.myself.guards

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StructureGuardTest {
    @Test fun kotlinFilesFollowStructure() {
        val src = if (File("src/main/java").isDirectory) File("src") else File("app/src")
        val violations = src.listFiles().orEmpty().filter { it.isDirectory }.flatMap { sourceSet ->
            val root = File(sourceSet, "java/com/menash/myself")
            val allowed = setOf("core", "feature", "shell", "e2e") +
                if (sourceSet.name == "test") setOf("testing", "guards") else emptySet()
            root.walkTopDown().filter { it.isFile && it.extension == "kt" }.mapNotNull { file ->
                val path = file.relativeTo(root).invariantSeparatorsPath
                val valid = if ('/' !in path) {
                    sourceSet.name == "main" && path in setOf("MainActivity.kt", "MyselfApp.kt")
                } else {
                    path.substringBefore('/') in allowed
                }
                if (valid) null else file.path
            }.toList()
        }
        assertTrue("Kotlin files outside the allowed structure:\n${violations.joinToString("\n")}", violations.isEmpty())
    }
}
