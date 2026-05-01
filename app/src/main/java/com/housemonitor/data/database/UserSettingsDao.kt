package com.housemonitor.data.database

import androidx.room.*
import com.housemonitor.data.model.UserSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getUserSettings(): Flow<UserSettings>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getUserSettingsSync(): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(settings: UserSettings)

    @Update
    suspend fun updateUserSettings(settings: UserSettings)

    @Query("UPDATE user_settings SET checkInterval = :interval, lastUpdated = :timestamp WHERE id = 1")
    suspend fun updateCheckInterval(interval: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_settings SET notificationEnabled = :enabled, lastUpdated = :timestamp WHERE id = 1")
    suspend fun updateNotificationEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
}