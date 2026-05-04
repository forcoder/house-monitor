package com.housemonitor.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.housemonitor.data.repository.PropertyRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class PropertyMonitorWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val propertyRepository: PropertyRepository,
    private val monitorUseCase: MonitorUseCase
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val UNIQUE_WORK_NAME = "property_monitor_work"
        const val KEY_PROPERTY_ID = "property_id"
        const val KEY_CHECK_DATE = "check_date"

        fun createInputData(propertyId: String? = null, checkDate: String? = null): Data {
            val builder = Data.Builder()
            propertyId?.let { builder.putString(KEY_PROPERTY_ID, it) }
            checkDate?.let { builder.putString(KEY_CHECK_DATE, it) }
            return builder.build()
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val propertyId = inputData.getString(KEY_PROPERTY_ID)
            val checkDate = inputData.getString(KEY_CHECK_DATE)

            if (propertyId != null) {
                val property = propertyRepository.getPropertyById(propertyId)
                if (property != null && property.isActive) {
                    monitorUseCase.execute(property, checkDate ?: getToday())
                }
            } else {
                val properties = propertyRepository.getActiveProperties().first()
                properties.forEach { property ->
                    try {
                        monitorUseCase.execute(property, checkDate ?: getToday())
                    } catch (_: Exception) {
                        // 单个失败不影响其他
                    }
                    kotlinx.coroutines.delay(2000)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun getToday(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}
