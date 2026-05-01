package com.housemonitor.data.repository

import com.housemonitor.data.database.UserSettingsDao
import com.housemonitor.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSettingsRepository @Inject constructor(
    private val userSettingsDao: UserSettingsDao
) {
    fun getUserSettings(): Flow<UserSettings> = userSettingsDao.getUserSettings()

    suspend fun getUserSettingsSync(): UserSettings? = userSettingsDao.getUserSettingsSync()

    suspend fun updateCheckInterval(interval: Int): Result<Unit> {
        return try {
            userSettingsDao.updateCheckInterval(interval)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNotificationEnabled(enabled: Boolean): Result<Unit> {
        return try {
            userSettingsDao.updateNotificationEnabled(enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun initializeDefaultSettings(): Result<UserSettings> {
        return try {
            val existingSettings = userSettingsDao.getUserSettingsSync()
            if (existingSettings == null) {
                val defaultSettings = UserSettings()
                userSettingsDao.insertUserSettings(defaultSettings)
                Result.success(defaultSettings)
            } else {
                Result.success(existingSettings)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateQuietHours(startHour: Int, endHour: Int): Result<Unit> {
        return try {
            val currentSettings = userSettingsDao.getUserSettingsSync()
            currentSettings?.let {
                val updatedSettings = it.copy(
                    quietHoursStart = startHour,
                    quietHoursEnd = endHour,
                    lastUpdated = System.currentTimeMillis()
                )
                userSettingsDao.updateUserSettings(updatedSettings)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}