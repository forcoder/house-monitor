package com.housemonitor.data.database

import androidx.room.*
import com.housemonitor.data.model.Property
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {
    @Query("SELECT * FROM properties ORDER BY createdAt DESC")
    fun getAllProperties(): Flow<List<Property>>

    @Query("SELECT * FROM properties WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveProperties(): Flow<List<Property>>

    @Query("SELECT * FROM properties WHERE id = :id")
    suspend fun getPropertyById(id: String): Property?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperty(property: Property)

    @Update
    suspend fun updateProperty(property: Property)

    @Delete
    suspend fun deleteProperty(property: Property)

    @Query("DELETE FROM properties WHERE id = :id")
    suspend fun deletePropertyById(id: String)

    @Query("UPDATE properties SET isActive = :isActive WHERE id = :id")
    suspend fun updatePropertyActive(id: String, isActive: Boolean)

    @Query("UPDATE properties SET lastCheckedAt = :timestamp WHERE id = :id")
    suspend fun updateLastCheckedAt(id: String, timestamp: Long)
}