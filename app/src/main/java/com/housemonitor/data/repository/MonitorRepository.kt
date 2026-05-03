package com.housemonitor.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.housemonitor.data.database.MonitorRecordDao
import com.housemonitor.data.model.MonitorRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitorRepository @Inject constructor(
    private val monitorRecordDao: MonitorRecordDao,
    private val gson: Gson
) {
    fun getRecordsByPropertyId(propertyId: String): Flow<List<MonitorRecord>> {
        return monitorRecordDao.getRecordsByPropertyId(propertyId)
    }

    fun getRecentRecords(limit: Int = 50): Flow<List<MonitorRecord>> {
        return monitorRecordDao.getRecentRecords(limit)
    }

    suspend fun getRecordByPropertyAndDate(propertyId: String, checkDate: String): MonitorRecord? {
        return monitorRecordDao.getRecordByPropertyAndDate(propertyId, checkDate)
    }

    suspend fun getLastSuccessRecord(propertyId: String): MonitorRecord? {
        return monitorRecordDao.getLastSuccessRecord(propertyId)
    }

    suspend fun saveMonitorRecord(
        propertyId: String,
        checkDate: String,
        unavailableDates: List<String>,
        status: String = "success"
    ): Result<MonitorRecord> {
        return try {
            val record = MonitorRecord(
                propertyId = propertyId,
                checkDate = checkDate,
                unavailableDates = gson.toJson(unavailableDates),
                status = status
            )
            monitorRecordDao.insertRecord(record)
            Result.success(record)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseUnavailableDates(jsonString: String): List<String> {
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun cleanupOldRecords(beforeDays: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (beforeDays * 24 * 60 * 60 * 1000L)
        monitorRecordDao.deleteOldRecords(cutoffTime)
    }

    suspend fun deleteRecordsByPropertyId(propertyId: String) {
        monitorRecordDao.deleteRecordsByPropertyId(propertyId)
    }
}