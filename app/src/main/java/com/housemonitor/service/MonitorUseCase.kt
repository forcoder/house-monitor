package com.housemonitor.service

import android.content.Context
import com.housemonitor.data.model.ChangeSummary
import com.housemonitor.data.model.ChangeType
import com.housemonitor.data.model.MonitorRecord
import com.housemonitor.data.model.Property
import com.housemonitor.data.model.UserSettings
import com.housemonitor.data.repository.MonitorRepository
import com.housemonitor.data.repository.PropertyRepository
import com.housemonitor.data.repository.UserSettingsRepository
import com.housemonitor.util.NotificationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class MonitorResult(
    val record: MonitorRecord,
    val changeSummary: ChangeSummary,
    val unavailableDates: List<String>
)

@Singleton
class MonitorUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val propertyRepository: PropertyRepository,
    private val monitorRepository: MonitorRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val notificationManager: NotificationManager,
    private val platformParserFactory: PlatformParserFactory
) {
    suspend fun execute(property: Property, checkDate: String = getCurrentDate()): MonitorResult? {
        if (!property.isActive) return null

        try {
            val unavailableDates = withContext(Dispatchers.Main) {
                val parser = platformParserFactory.getParser(property.platform)
                val meituanWebView = MeituanWebView(context, parser)
                meituanWebView.initialize()

                try {
                    meituanWebView.loadUrl(property.url)
                    delay(8000)
                    meituanWebView.evaluateCalendarStatus()
                } finally {
                    meituanWebView.destroy()
                }
            }

            val previousRecord = monitorRepository.getLastSuccessRecord(property.id)
            val previousDates = previousRecord?.let {
                monitorRepository.parseUnavailableDates(it.unavailableDates)
            } ?: emptyList()

            val changeSummary = computeChangeSummary(previousDates, unavailableDates)

            val record = monitorRepository.saveMonitorRecord(
                propertyId = property.id,
                checkDate = checkDate,
                unavailableDates = unavailableDates,
                status = "success",
                changeSummary = changeSummary.toJson()
            ).getOrThrow()

            propertyRepository.updateLastCheckedAt(property.id, System.currentTimeMillis())

            checkForStatusChanges(property, changeSummary)

            monitorRepository.cleanupOldRecordsByPropertyId(property.id)

            return MonitorResult(record, changeSummary, unavailableDates)
        } catch (e: Exception) {
            monitorRepository.saveMonitorRecord(
                propertyId = property.id,
                checkDate = checkDate,
                unavailableDates = emptyList(),
                status = "failed",
                changeSummary = ""
            )
            return null
        }
    }

    private suspend fun checkForStatusChanges(
        property: Property,
        changeSummary: ChangeSummary
    ) {
        val userSettings = userSettingsRepository.getUserSettingsSync()
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

    private fun isInQuietHours(settings: UserSettings): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        return if (settings.quietHoursStart < settings.quietHoursEnd) {
            currentHour >= settings.quietHoursStart && currentHour < settings.quietHoursEnd
        } else {
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

    private fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }
}
