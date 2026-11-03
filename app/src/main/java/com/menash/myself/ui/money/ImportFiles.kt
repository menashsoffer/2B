package com.menash.myself.ui.money

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.IntentCompat
import com.menash.myself.feature.money.importer.ImportFile
import com.menash.myself.feature.money.importer.MAX_FILE_BYTES

/** Mime types the share sheet and the picker offer for bank and card exports. */
val importMimeTypes = arrayOf(
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-excel.sheet.macroEnabled.12",
    "application/octet-stream",
)

/** Content URIs carried by a share (one or several files) or an "open with"; empty for any other intent. */
fun sharedFileUris(intent: Intent?): List<Uri> = when (intent?.action) {
    Intent.ACTION_SEND -> listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
    Intent.ACTION_SEND_MULTIPLE ->
        IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty().filterNotNull()
    Intent.ACTION_VIEW -> listOfNotNull(intent.data)
    else -> emptyList()
}.filter { it.scheme == ContentResolver.SCHEME_CONTENT }

/** Reads one picked or shared file. Files over the size limit are cut there, and the reader refuses them. */
fun readImportFile(resolver: ContentResolver, uri: Uri): ImportFile {
    val name = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment.orEmpty()
    return runCatching {
        val bytes = requireNotNull(resolver.openInputStream(uri)).use { input ->
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            var total = 0
            while (total <= MAX_FILE_BYTES) {
                val n = input.read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
                total += n
            }
            out.toByteArray()
        }
        ImportFile(name, bytes)
    }.getOrElse { ImportFile(name, byteArrayOf(), readError = true) }
}

internal suspend fun readImportFiles(resolver: ContentResolver, uris: List<Uri>): List<ImportFile> =
    withContext(Dispatchers.IO) { uris.map { readImportFile(resolver, it) } }
