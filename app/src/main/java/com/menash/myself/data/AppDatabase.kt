package com.menash.myself.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.menash.myself.data.dao.AppDao
import androidx.room.migration.Migration
import com.menash.myself.data.entities.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AppSettings::class,
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** The legacy calculators moved into money (31/10, approved by Menash); their tables go. Settings stay. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS cars")
                db.execSQL("DROP TABLE IF EXISTS ai_subscriptions")
                db.execSQL("DROP TABLE IF EXISTS pension_funds")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_calculators.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed the default settings row
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getDatabase(context).appDao()

                            // Default Settings
                            dao.saveSettings(AppSettings(id = 1, usdToIlsRate = 3.11, lastBackupTimestamp = 0L))
                        }
                    }
                }).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
