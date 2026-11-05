package com.menash.myself.feature.money.ui

import android.content.Intent
import android.net.Uri
import com.menash.myself.feature.money.ImportInbox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Files shared from WhatsApp, mail or Files reach the import once. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ImportShareTest {

    private val xlsx = Uri.parse("content://com.whatsapp.provider.media/item/1")
    private val xlsm = Uri.parse("content://com.google.android.gm.sapi/attachment/2")

    @Test
    fun sendOneFile() {
        val intent = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, xlsx).setType("application/vnd.ms-excel")
        assertEquals(listOf(xlsx), sharedFileUris(intent))
    }

    @Test
    fun sendSeveralFiles() {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(xlsx, xlsm))
        assertEquals(listOf(xlsx, xlsm), sharedFileUris(intent))
    }

    @Test
    fun openWith() {
        assertEquals(listOf(xlsm), sharedFileUris(Intent(Intent.ACTION_VIEW, xlsm)))
    }

    @Test
    fun otherIntentsAndFileUrisAreIgnored() {
        assertTrue(sharedFileUris(Intent(Intent.ACTION_MAIN)).isEmpty())
        assertTrue(sharedFileUris(null).isEmpty())
        assertTrue(sharedFileUris(Intent(Intent.ACTION_VIEW, Uri.parse("file:///sdcard/x.xlsx"))).isEmpty())
        assertTrue(sharedFileUris(Intent(Intent.ACTION_SEND)).isEmpty())
    }

    @Test
    fun inboxHandsEachFileOverOnce() {
        val inbox = ImportInbox()
        inbox.offer(listOf(xlsx.toString()))
        inbox.offer(listOf(xlsx.toString(), xlsm.toString()))
        assertEquals(listOf(xlsx.toString(), xlsm.toString()), inbox.take())
        assertTrue(inbox.take().isEmpty())
        assertTrue(inbox.pending.value.isEmpty())
    }
}
