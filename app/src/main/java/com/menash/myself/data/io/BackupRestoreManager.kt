package com.menash.myself.data.io

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.menash.myself.data.AppDatabase
import com.menash.myself.data.entities.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupRestoreManager {

    suspend fun createAndShareBackup(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val dao = db.appDao()

        val settings = dao.getSettings() ?: AppSettings()

        val root = JSONObject()
        root.put("appName", "המחשבונים שלי")
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        root.put("exportDate", dateFormat.format(Date()))
        root.put("version", 1)

        // Settings
        val settingsObj = JSONObject()
        settingsObj.put("usdToIlsRate", settings.usdToIlsRate)
        settingsObj.put("lastBackupTimestamp", System.currentTimeMillis())
        root.put("settings", settingsObj)

        // Write to cache file
        val backupDir = File(context.cacheDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        val backupFile = File(backupDir, "my_calculators_backup.json")
        backupFile.writeText(root.toString(2))

        // Update last backup timestamp
        dao.saveSettings(settings.copy(lastBackupTimestamp = System.currentTimeMillis()))

        // Share via Intent
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "גיבוי אפליקציית המחשבונים שלי")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "שמור או שתף קובץ גיבוי")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    suspend fun restoreFromJson(context: Context, jsonString: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val root = JSONObject(jsonString)
            val db = AppDatabase.getDatabase(context)
            val dao = db.appDao()

            // Settings
            if (root.has("settings")) {
                val sObj = root.getJSONObject("settings")
                val rate = sObj.optDouble("usdToIlsRate", 3.11)
                dao.saveSettings(AppSettings(id = 1, usdToIlsRate = rate, lastBackupTimestamp = System.currentTimeMillis()))
            }

            // Older backups also hold the legacy calculators; those moved into money and are ignored here.
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
