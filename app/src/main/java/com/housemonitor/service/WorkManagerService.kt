package com.housemonitor.service

import android.content.Context
import androidx.work.*
import com.housemonitor.data.repository.UserSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userSettingsRepository: UserSettingsRepository
) {

    private val workManager = WorkManager.getInstance(context)

    suspend fun schedulePeriodicMonitoring() {
        val settings = userSettingsRepository.getUserSettingsSync()
        settings?.let {
            val intervalMinutes = it.checkInterval.coerceAtLeast(5) // 最小5分钟

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<PropertyMonitorWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES,
                5, TimeUnit.MINUTES // 弹性时间
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                PropertyMonitorWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    suspend fun scheduleImmediateMonitoring(propertyId: String? = null) {
        val inputData = if (propertyId != null) {
            PropertyMonitorWorker.createInputData(propertyId = propertyId)
        } else {
            PropertyMonitorWorker.createInputData()
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<PropertyMonitorWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                30, TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            "immediate_monitor_${System.currentTimeMillis()}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelPeriodicMonitoring() {
        workManager.cancelUniqueWork(PropertyMonitorWorker.UNIQUE_WORK_NAME)
    }

    fun cancelAllMonitoring() {
        workManager.cancelAllWork()
    }

    suspend fun updateMonitoringInterval() {
        // 取消现有的周期性任务
        cancelPeriodicMonitoring()
        // 重新安排新的周期性任务
        schedulePeriodicMonitoring()
    }

    fun getWorkInfoById(workId: String) {
        // 可以获取工作状态信息
        workManager.getWorkInfosForUniqueWorkLiveData(workId)
    }
}