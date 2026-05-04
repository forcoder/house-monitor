package com.housemonitor.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.housemonitor.data.model.ChangeSummary
import com.housemonitor.data.model.ChangeType
import com.housemonitor.data.repository.MonitorRepository
import com.housemonitor.data.repository.PropertyRepository
import com.housemonitor.data.repository.UserSettingsRepository
import com.housemonitor.util.NotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@HiltWorker
class PropertyMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val propertyRepository: PropertyRepository,
    private val monitorRepository: MonitorRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val notificationManager: NotificationManager,
    private val platformParserFactory: PlatformParserFactory
) : CoroutineWorker(context, workerParams) {

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
            val checkDate = inputData.getString(KEY_CHECK_DATE) ?: getCurrentDate()

            if (propertyId != null) {
                // 监控单个房源
                monitorSingleProperty(propertyId, checkDate)
            } else {
                // 监控所有活跃房源
                monitorAllProperties(checkDate)
            }

            Result.success()
        } catch (e: Exception) {
            // 记录错误日志
            Result.retry()
        }
    }

    private suspend fun monitorSingleProperty(propertyId: String, checkDate: String) {
        val property = propertyRepository.getPropertyById(propertyId)
        if (property == null || !property.isActive) {
            return
        }

        try {
            // WebView 必须在主线程上创建和操作
            val unavailableDates = withContext(Dispatchers.Main) {
                val parser = platformParserFactory.getParser(property.platform)
                val meituanWebView = MeituanWebView(applicationContext, parser)
                meituanWebView.initialize()

                try {
                    // 加载页面
                    meituanWebView.loadUrl(property.url)

                    // 等待页面加载完成
                    delay(8000) // 8秒等待页面完全加载

                    // 检测日历状态
                    meituanWebView.evaluateCalendarStatus()
                } finally {
                    meituanWebView.destroy()
                }
            }

            // 获取最近一次成功检查的记录用于比较（不限日期，确保能检测到变化）
            val previousRecord = monitorRepository.getLastSuccessRecord(propertyId)
            val previousDates = previousRecord?.let { monitorRepository.parseUnavailableDates(it.unavailableDates) } ?: emptyList()

            // 计算变化摘要
            val changeSummary = computeChangeSummary(previousDates, unavailableDates)

            // 保存监控记录
            monitorRepository.saveMonitorRecord(
                propertyId = property.id,
                checkDate = checkDate,
                unavailableDates = unavailableDates,
                status = "success",
                changeSummary = changeSummary.toJson()
            )

            // 更新最后检查时间
            propertyRepository.updateLastCheckedAt(property.id, System.currentTimeMillis())

            // 检查是否有状态变化并发送相应通知
            checkForStatusChanges(
                property = property,
                changeSummary = changeSummary,
                userSettings = userSettingsRepository.getUserSettingsSync()
            )

            // 清理旧记录（每房源只保留最近 10 条）
            monitorRepository.cleanupOldRecordsByPropertyId(property.id)

        } catch (e: Exception) {
            // 记录失败状态
            monitorRepository.saveMonitorRecord(
                propertyId = property.id,
                checkDate = checkDate,
                unavailableDates = emptyList(),
                status = "failed",
                changeSummary = ""
            )
        }
    }

    /**
     * 检查房源状态变化并发送相应的通知
     */
    private suspend fun checkForStatusChanges(
        property: com.housemonitor.data.model.Property,
        changeSummary: ChangeSummary,
        userSettings: com.housemonitor.data.model.UserSettings?
    ) {
        if (userSettings?.notificationEnabled != true) return
        if (userSettings != null && isInQuietHours(userSettings)) return
        if (changeSummary.changeType == ChangeType.NO_CHANGE.name) return

        val changeType = when (changeSummary.changeType) {
            ChangeType.BECAME_UNAVAILABLE.name -> NotificationManager.StatusChangeType.BECAME_UNAVAILABLE
            ChangeType.BECAME_AVAILABLE.name -> NotificationManager.StatusChangeType.BECAME_AVAILABLE
            else -> NotificationManager.StatusChangeType.PARTIAL_CHANGE
        }
        val dates = changeSummary.newlyUnavailable + changeSummary.newlyAvailable

        notificationManager.showStatusChangeNotification(
            propertyName = property.name,
            changeType = changeType,
            dates = dates
        )
    }

    private suspend fun monitorAllProperties(checkDate: String) {
        val properties = propertyRepository.getActiveProperties().first()

        properties.forEach { property ->
            monitorSingleProperty(property.id, checkDate)
            // 避免过于频繁的请求
            delay(2000)
        }
    }

    private fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun isInQuietHours(settings: com.housemonitor.data.model.UserSettings): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        return if (settings.quietHoursStart < settings.quietHoursEnd) {
            // 同一天的时间段
            currentHour >= settings.quietHoursStart && currentHour < settings.quietHoursEnd
        } else {
            // 跨天的时间段
            currentHour >= settings.quietHoursStart || currentHour < settings.quietHoursEnd
        }
    }

    private fun computeChangeSummary(
        previousDates: List<String>,
        currentDates: List<String>
    ): ChangeSummary {
        val newlyUnavailable = currentDates - previousDates.toSet()
        val newlyAvailable = previousDates - currentDates.toSet()

        return when {
            newlyUnavailable.isNotEmpty() && newlyAvailable.isNotEmpty() -> {
                ChangeSummary(newlyUnavailable, newlyAvailable, ChangeType.PARTIAL_CHANGE.name)
            }
            newlyUnavailable.isNotEmpty() -> {
                ChangeSummary(newlyUnavailable, emptyList(), ChangeType.BECAME_UNAVAILABLE.name)
            }
            newlyAvailable.isNotEmpty() -> {
                ChangeSummary(emptyList(), newlyAvailable, ChangeType.BECAME_AVAILABLE.name)
            }
            else -> ChangeSummary.noChange()
        }
    }

}