package com.housemonitor.data.repository

import com.housemonitor.data.database.PropertyDao
import com.housemonitor.data.model.Property
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PropertyRepository @Inject constructor(
    private val propertyDao: PropertyDao
) {
    fun getAllProperties(): Flow<List<Property>> = propertyDao.getAllProperties()

    fun getActiveProperties(): Flow<List<Property>> = propertyDao.getActiveProperties()

    suspend fun getPropertyById(id: String): Property? = propertyDao.getPropertyById(id)

    suspend fun insertProperty(property: Property) = propertyDao.insertProperty(property)

    suspend fun updateProperty(property: Property) = propertyDao.updateProperty(property)

    suspend fun deleteProperty(property: Property) = propertyDao.deleteProperty(property)

    suspend fun deletePropertyById(id: String) = propertyDao.deletePropertyById(id)

    suspend fun updatePropertyActive(id: String, isActive: Boolean) {
        propertyDao.updatePropertyActive(id, isActive)
    }

    suspend fun updateLastCheckedAt(id: String, timestamp: Long) {
        propertyDao.updateLastCheckedAt(id, timestamp)
    }

    suspend fun addProperty(name: String, url: String, description: String = "", platform: String = "meituan"): Result<Property> {
        return try {
            val property = Property(
                name = name,
                url = url,
                description = description,
                platform = platform
            )
            propertyDao.insertProperty(property)
            Result.success(property)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeProperty(id: String): Result<Unit> {
        return try {
            propertyDao.deletePropertyById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}