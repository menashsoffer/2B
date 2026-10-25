package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AppDao
import com.example.data.entities.AiSubscriptionEntity
import com.example.data.entities.AppSettings
import com.example.data.entities.CarEntity
import com.example.data.entities.PensionFundEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AppSettings::class,
        CarEntity::class,
        AiSubscriptionEntity::class,
        PensionFundEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_calculators.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed initial deletable example rows for all calculators
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getDatabase(context).appDao()

                            // Default Settings
                            dao.saveSettings(AppSettings(id = 1, usdToIlsRate = 3.11, lastBackupTimestamp = 0L))

                            // Calculator 1: Car comparison example
                            dao.insertCar(
                                CarEntity(
                                    name = "טויוטה קורולה היברידית",
                                    purchasePrice = 148000.0,
                                    resaleValueAfter5Years = 88000.0,
                                    energyCostYearly = 7200.0,
                                    insuranceYearly = 4800.0,
                                    trackerYearly = 650.0,
                                    maintenanceYearly = 1800.0,
                                    annualTestYearly = 1950.0,
                                    tires5YearsTotal = 2400.0,
                                    isBaseline = true
                                )
                            )

                            // Calculator 2: AI Subscriptions example
                            dao.insertAiSubscription(
                                AiSubscriptionEntity(
                                    name = "ChatGPT Plus",
                                    category = "מודלי שפה (LLM)",
                                    isActive = true,
                                    billingCycle = "MONTHLY",
                                    currency = "USD",
                                    price = 20.0,
                                    notes = "כתיבה, תכנות ומחקר שוטף"
                                )
                            )

                            // Calculator 3: Pension & Savings example rows
                            dao.insertPensionFund(
                                PensionFundEntity(
                                    fundType = "קרן השתלמות",
                                    provider = "אלטשולר שחם",
                                    currentValue = 145000.0,
                                    totalDeposits = 110000.0,
                                    feeOnBalancePercent = 0.28,
                                    feeOnDepositsPercent = 0.0,
                                    notes = "מסלול מחקה S&P 500"
                                )
                            )

                            dao.insertPensionFund(
                                PensionFundEntity(
                                    fundType = "קרן פנסיה",
                                    provider = "מנורה מבטחים",
                                    currentValue = 320000.0,
                                    totalDeposits = 240000.0,
                                    feeOnBalancePercent = 0.19,
                                    feeOnDepositsPercent = 1.49,
                                    notes = "פנסיה מקיפה מסלול כללי"
                                )
                            )
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
