package com.menash.myself

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PackageMatchesDirectoryTest {
    @Test fun packagesMatchDirectories() {
        val src = if (File("src/main/java").isDirectory) File("src") else File("app/src")
        val packageLine = Regex("(?m)^\\s*package\\s+([\\w.]+)")
        val violations = src.listFiles().orEmpty().filter { it.isDirectory }.flatMap { sourceSet ->
            val javaDir = File(sourceSet, "java")
            javaDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.mapNotNull { file ->
                val expected = file.parentFile.relativeTo(javaDir).invariantSeparatorsPath.replace('/', '.')
                val actual = packageLine.find(file.readText())?.groupValues?.get(1)
                if (actual == expected) null else "${file.path}: expected $expected, found $actual"
            }.toList()
        }
        assertTrue("Package does not match directory:\n${violations.joinToString("\n")}", violations.isEmpty())
    }
}
