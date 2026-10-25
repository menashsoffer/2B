package com.menash.myself.data.io

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.menash.myself.data.AppDatabase
import com.menash.myself.data.entities.AiSubscriptionEntity
import com.menash.myself.data.entities.AppSettings
import com.menash.myself.data.entities.CarEntity
import com.menash.myself.data.entities.PensionFundEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
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
        val cars = dao.getAllCars()
        val aiSubs = dao.getAllAiSubscriptions()
        val pensionFunds = dao.getAllPensionFunds()

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

        // Cars
        val carsArr = JSONArray()
        for (car in cars) {
            val c = JSONObject()
            c.put("name", car.name)
            c.put("purchasePrice", car.purchasePrice)
            c.put("resaleValueAfter5Years", car.resaleValueAfter5Years)
            c.put("energyCostYearly", car.energyCostYearly)
            c.put("insuranceYearly", car.insuranceYearly)
            c.put("trackerYearly", car.trackerYearly)
            c.put("maintenanceYearly", car.maintenanceYearly)
            c.put("annualTestYearly", car.annualTestYearly)
            c.put("tires5YearsTotal", car.tires5YearsTotal)
            c.put("isBaseline", car.isBaseline)
            carsArr.put(c)
        }
        root.put("cars", carsArr)

        // AI Subscriptions
        val aiArr = JSONArray()
        for (sub in aiSubs) {
            val s = JSONObject()
            s.put("name", sub.name)
            s.put("category", sub.category)
            s.put("isActive", sub.isActive)
            s.put("billingCycle", sub.billingCycle)
            s.put("currency", sub.currency)
            s.put("price", sub.price)
            s.put("notes", sub.notes)
            aiArr.put(s)
        }
        root.put("aiSubscriptions", aiArr)

        // Pension Funds
        val pensionArr = JSONArray()
        for (fund in pensionFunds) {
            val p = JSONObject()
            p.put("fundType", fund.fundType)
            p.put("provider", fund.provider)
            p.put("currentValue", fund.currentValue)
            p.put("totalDeposits", fund.totalDeposits)
            p.put("feeOnBalancePercent", fund.feeOnBalancePercent)
            p.put("feeOnDepositsPercent", fund.feeOnDepositsPercent)
            p.put("notes", fund.notes)
            pensionArr.put(p)
        }
        root.put("pensionFunds", pensionArr)

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

            // Cars
            if (root.has("cars")) {
                val carsArr = root.getJSONArray("cars")
                val carsList = mutableListOf<CarEntity>()
                for (i in 0 until carsArr.length()) {
                    val obj = carsArr.getJSONObject(i)
                    carsList.add(
                        CarEntity(
                            name = obj.optString("name", "רכב מיובא"),
                            purchasePrice = obj.optDouble("purchasePrice", 100000.0),
                            resaleValueAfter5Years = obj.optDouble("resaleValueAfter5Years", 50000.0),
                            energyCostYearly = obj.optDouble("energyCostYearly", 6000.0),
                            insuranceYearly = obj.optDouble("insuranceYearly", 4000.0),
                            trackerYearly = obj.optDouble("trackerYearly", 600.0),
                            maintenanceYearly = obj.optDouble("maintenanceYearly", 1500.0),
                            annualTestYearly = obj.optDouble("annualTestYearly", 1800.0),
                            tires5YearsTotal = obj.optDouble("tires5YearsTotal", 2000.0),
                            isBaseline = obj.optBoolean("isBaseline", false)
                        )
                    )
                }
                if (carsList.isNotEmpty()) {
                    dao.insertCars(carsList)
                }
            }

            // AI Subscriptions
            if (root.has("aiSubscriptions")) {
                val aiArr = root.getJSONArray("aiSubscriptions")
                val aiList = mutableListOf<AiSubscriptionEntity>()
                for (i in 0 until aiArr.length()) {
                    val obj = aiArr.getJSONObject(i)
                    aiList.add(
                        AiSubscriptionEntity(
                            name = obj.optString("name", "מנוי"),
                            category = obj.optString("category", "כללי"),
                            isActive = obj.optBoolean("isActive", true),
                            billingCycle = obj.optString("billingCycle", "MONTHLY"),
                            currency = obj.optString("currency", "USD"),
                            price = obj.optDouble("price", 10.0),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
                if (aiList.isNotEmpty()) {
                    dao.insertAiSubscriptions(aiList)
                }
            }

            // Pension Funds
            if (root.has("pensionFunds")) {
                val pensionArr = root.getJSONArray("pensionFunds")
                val pensionList = mutableListOf<PensionFundEntity>()
                for (i in 0 until pensionArr.length()) {
                    val obj = pensionArr.getJSONObject(i)
                    pensionList.add(
                        PensionFundEntity(
                            fundType = obj.optString("fundType", "קרן השתלמות"),
                            provider = obj.optString("provider", "חברה מנהלת"),
                            currentValue = obj.optDouble("currentValue", 0.0),
                            totalDeposits = obj.optDouble("totalDeposits", 0.0),
                            feeOnBalancePercent = obj.optDouble("feeOnBalancePercent", 0.2),
                            feeOnDepositsPercent = obj.optDouble("feeOnDepositsPercent", 0.0),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
                if (pensionList.isNotEmpty()) {
                    dao.insertPensionFunds(pensionList)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
