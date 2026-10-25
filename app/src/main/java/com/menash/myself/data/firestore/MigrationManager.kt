package com.menash.myself.data.firestore

import android.content.Context
import android.util.Log
import com.menash.myself.data.AppDatabase
import com.menash.myself.data.io.BackupRestoreManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object MigrationManager {
    private const val TAG = "MigrationManager"
    private const val PREFS_NAME = "life_hub_migration"

    suspend fun checkAndMigrate(context: Context): Boolean = withContext(Dispatchers.IO) {
        val auth = Firebase.auth
        val uid = auth.currentUser?.uid ?: return@withContext false

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "migrated_$uid"
        if (prefs.getBoolean(key, false)) {
            Log.d(TAG, "Migration already completed for $uid")
            return@withContext true
        }

        try {
            val db = AppDatabase.getDatabase(context)
            val dao = db.appDao()

            val cars = dao.getAllCars()
            val aiSubs = dao.getAllAiSubscriptions()
            val pensionFunds = dao.getAllPensionFunds()

            Log.i(TAG, "Starting non-destructive migration: ${cars.size} cars, ${aiSubs.size} AI subs, ${pensionFunds.size} pension funds")

            // 1. NON-DESTRUCTIVE: Back up locally first to a safety file
            val localBackupDir = File(context.filesDir, "pre_migration_backups")
            if (!localBackupDir.exists()) localBackupDir.mkdirs()
            val backupFile = File(localBackupDir, "backup_before_migration_${System.currentTimeMillis()}.json")
            
            val backupJson = JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("carsCount", cars.size)
                put("aiCount", aiSubs.size)
                put("pensionCount", pensionFunds.size)
            }
            backupFile.writeText(backupJson.toString())
            Log.i(TAG, "Saved pre-migration local backup to: ${backupFile.absolutePath}")

            // 2. Upload to Firestore
            val firestore = FirestoreManager.get(context)

            // Cars sync
            val carsArr = JSONArray()
            cars.forEach { c ->
                carsArr.put(JSONObject().apply {
                    put("name", c.name)
                    put("purchasePrice", c.purchasePrice)
                    put("resaleValueAfter5Years", c.resaleValueAfter5Years)
                    put("energyCostYearly", c.energyCostYearly)
                    put("netMonthlyCost", c.netMonthlyCost)
                })
            }
            val carDoc = firestore.collection("calculator_sync").document("cars_$uid")
            carDoc.set(
                mapOf(
                    "calculatorType" to "cars",
                    "recordsCount" to cars.size,
                    "dataJson" to carsArr.toString(),
                    "userId" to uid,
                    "space" to "shared",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // AI Subscriptions sync
            val aiArr = JSONArray()
            aiSubs.forEach { s ->
                aiArr.put(JSONObject().apply {
                    put("name", s.name)
                    put("category", s.category)
                    put("price", s.price)
                    put("currency", s.currency)
                    put("billingCycle", s.billingCycle)
                    put("isActive", s.isActive)
                })
            }
            val aiDoc = firestore.collection("calculator_sync").document("ai_$uid")
            aiDoc.set(
                mapOf(
                    "calculatorType" to "ai_subscriptions",
                    "recordsCount" to aiSubs.size,
                    "dataJson" to aiArr.toString(),
                    "userId" to uid,
                    "space" to "shared",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Pension Funds sync
            val pensionArr = JSONArray()
            pensionFunds.forEach { p ->
                pensionArr.put(JSONObject().apply {
                    put("fundType", p.fundType)
                    put("provider", p.provider)
                    put("currentValue", p.currentValue)
                    put("totalDeposits", p.totalDeposits)
                    put("profit", p.profit)
                    put("feeOnBalancePercent", p.feeOnBalancePercent)
                })
            }
            val pensionDoc = firestore.collection("calculator_sync").document("pension_$uid")
            pensionDoc.set(
                mapOf(
                    "calculatorType" to "pension_savings",
                    "recordsCount" to pensionFunds.size,
                    "dataJson" to pensionArr.toString(),
                    "userId" to uid,
                    "space" to "shared",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // 3. Verification: Read back from Firestore to verify count
            val readCar = carDoc.get().await()
            val readAi = aiDoc.get().await()
            val readPension = pensionDoc.get().await()

            val carCount = readCar.getLong("recordsCount")?.toInt() ?: -1
            val aiCount = readAi.getLong("recordsCount")?.toInt() ?: -1
            val pensionCount = readPension.getLong("recordsCount")?.toInt() ?: -1

            if (carCount == cars.size && aiCount == aiSubs.size && pensionCount == pensionFunds.size) {
                Log.i(TAG, "Migration verification SUCCESS: all counts match perfectly!")
                prefs.edit().putBoolean(key, true).apply()
                true
            } else {
                Log.w(TAG, "Migration verification failed: counts mismatch")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Migration encountered error", e)
            false
        }
    }
}
