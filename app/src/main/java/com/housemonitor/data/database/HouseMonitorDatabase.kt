package com.housemonitor.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.housemonitor.data.model.Property
import com.housemonitor.data.model.MonitorRecord
import com.housemonitor.data.model.UserSettings

@Database(
    entities = [Property::class, MonitorRecord::class, UserSettings::class],
    version = 3,
    exportSchema = false
)
abstract class HouseMonitorDatabase : RoomDatabase() {
    abstract fun propertyDao(): PropertyDao
    abstract fun monitorRecordDao(): MonitorRecordDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: HouseMonitorDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE properties ADD COLUMN platform TEXT NOT NULL DEFAULT 'meituan'"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE monitor_records ADD COLUMN changeSummary TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        fun getDatabase(context: Context): HouseMonitorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HouseMonitorDatabase::class.java,
                    "house_monitor_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
