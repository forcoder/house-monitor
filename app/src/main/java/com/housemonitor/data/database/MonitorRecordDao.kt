package com.housemonitor.data.database

import androidx.room.*
import com.housemonitor.data.model.MonitorRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitorRecordDao {
    @Query("SELECT * FROM monitor_records WHERE propertyId = :propertyId ORDER BY checkedAt DESC")
    fun getRecordsByPropertyId(propertyId: String): Flow<List<MonitorRecord>>

    @Query("SELECT * FROM monitor_records ORDER BY checkedAt DESC LIMIT :limit")
    fun getRecentRecords(limit: Int = 50): Flow<List<MonitorRecord>>

    @Query("SELECT * FROM monitor_records WHERE propertyId = :propertyId AND checkDate = :checkDate LIMIT 1")
    suspend fun getRecordByPropertyAndDate(propertyId: String, checkDate: String): MonitorRecord?

    @Query("SELECT * FROM monitor_records WHERE propertyId = :propertyId AND status = 'success' ORDER BY checkedAt DESC LIMIT 1")
    suspend fun getLastSuccessRecord(propertyId: String): MonitorRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MonitorRecord)

    @Update
    suspend fun updateRecord(record: MonitorRecord)

    @Query("DELETE FROM monitor_records WHERE checkedAt < :timestamp")
    suspend fun deleteOldRecords(timestamp: Long)

    @Query("DELETE FROM monitor_records WHERE propertyId = :propertyId")
    suspend fun deleteRecordsByPropertyId(propertyId: String)
}