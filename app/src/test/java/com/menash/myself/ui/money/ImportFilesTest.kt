package com.menash.myself.ui.money

import android.net.Uri
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ImportFilesTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun readsFileNameAndBytes() {
        val bytes = byteArrayOf(0, 1, 2, 42, -1)
        val file = temporaryFolder.newFile("statement.xlsx").apply { writeBytes(bytes) }

        val result = readImportFile(RuntimeEnvironment.getApplication().contentResolver, Uri.fromFile(file))

        assertEquals(file.name, result.name)
        assertArrayEquals(bytes, result.bytes)
        assertFalse(result.readError)
    }

    @Test
    fun missingFileReturnsReadErrorAndEmptyBytes() {
        val file = temporaryFolder.root.resolve("missing.xlsx")

        val result = readImportFile(RuntimeEnvironment.getApplication().contentResolver, Uri.fromFile(file))

        assertTrue(result.readError)
        assertArrayEquals(byteArrayOf(), result.bytes)
    }
}
