package com.menash.myself.feature.money.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * The rules guard: firestore-fixtures/money/{collection}/{case}.json must equal what the writers produce today.
 * When a writer changes, run the unit tests once with RECORD_FIXTURES=true, commit the JSON, and make
 * firestore.rules accept it (firestore.test.js loads every fixture).
 */
class MoneyFixturesTest {

    private val root = File("..").canonicalFile
    private val dir = File(root, "firestore-fixtures/money")
    private val record = System.getenv("RECORD_FIXTURES") == "true"

    @Test
    fun everyMoneyCollectionInTheRulesHasFixtures() {
        val rules = File(root, "firestore.rules").readText()
        val list = Regex("""function isMoneyCollection\(name\)\s*\{\s*return name in \[(.*?)]""", RegexOption.DOT_MATCHES_ALL)
            .find(rules)?.groupValues?.get(1) ?: error("isMoneyCollection not found in firestore.rules")
        val inRules = Regex("'([A-Za-z]+)'").findAll(list).map { it.groupValues[1] }.toSet()
        assertEquals(20, inRules.size)
        assertEquals(inRules, MoneyFixtures.all().keys)
    }

    @Test
    fun fixturesMatchTheWriters() {
        val expected = MoneyFixtures.all().flatMap { (collection, cases) ->
            cases.map { (case, doc) -> "$collection/$case.json" to Json.write(doc) + "\n" }
        }.toMap()
        if (record) {
            dir.deleteRecursively()
            expected.forEach { (path, json) -> File(dir, path).apply { parentFile.mkdirs() }.writeText(json) }
            return
        }
        val onDisk = dir.walkTopDown().filter { it.isFile && it.extension == "json" }
            .associate { it.relativeTo(dir).invariantSeparatorsPath to it.readText() }
        val problems = buildList {
            expected.forEach { (path, json) ->
                when (onDisk[path]) {
                    null -> add("missing $path")
                    json -> Unit
                    else -> add("changed $path:\n  now ${json.trim()}")
                }
            }
            (onDisk.keys - expected.keys).forEach { add("stale $it") }
        }
        if (problems.isNotEmpty()) {
            fail(
                "Money writers and firestore-fixtures/money differ. Re-record with RECORD_FIXTURES=true, " +
                    "commit, and update firestore.rules:\n" + problems.joinToString("\n"),
            )
        }
    }

    @Test
    fun everyWriterStampsTheSchemaVersion() {
        MoneyFixtures.all().forEach { (collection, cases) ->
            cases.forEach { (case, doc) -> assertEquals("$collection/$case", SchemaVersion.CURRENT, doc[SchemaVersion.KEY]) }
        }
    }

    @Test
    fun documentsStayWithinTheRulesKeyLimit() {
        MoneyFixtures.all().values.flatMap { it.values }.forEach { assertTrue(it.keys.toString(), it.size <= 20) }
    }
}

/** Minimal, stable JSON: sorted map keys, integers as integers, UTF-8 text as is. */
internal object Json {
    fun write(value: Any?): String = buildString { write(this, value) }

    private fun write(out: StringBuilder, value: Any?) {
        when (value) {
            null -> out.append("null")
            is String -> string(out, value)
            is Boolean -> out.append(value)
            is Long, is Int -> out.append(value)
            is Map<*, *> -> {
                out.append('{')
                value.entries.sortedBy { it.key as String }.forEachIndexed { i, (k, v) ->
                    if (i > 0) out.append(',')
                    string(out, k as String)
                    out.append(':')
                    write(out, v)
                }
                out.append('}')
            }
            is List<*> -> {
                out.append('[')
                value.forEachIndexed { i, v -> if (i > 0) out.append(','); write(out, v) }
                out.append(']')
            }
            else -> error("Unsupported value in a money document: ${value::class}")
        }
    }

    private fun string(out: StringBuilder, s: String) {
        out.append('"')
        s.forEach { c ->
            when {
                c == '"' -> out.append("\\\"")
                c == '\\' -> out.append("\\\\")
                c == '\n' -> out.append("\\n")
                c < ' ' -> out.append("\\u%04x".format(c.code))
                else -> out.append(c)
            }
        }
        out.append('"')
    }
}
