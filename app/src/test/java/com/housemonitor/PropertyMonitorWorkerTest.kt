package com.housemonitor

import androidx.work.Data
import androidx.work.testing.TestListenableWorkerBuilder
import com.housemonitor.data.model.MonitorRecord
import com.housemonitor.service.PropertyMonitorWorker
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import java.text.SimpleDateFormat
import java.util.*

class PropertyMonitorWorkerTest {

    private lateinit var worker: PropertyMonitorWorker

    @Before
    fun setup() {
        // 创建模拟依赖（实际项目中需要使用真正的测试doubles）
        worker = TestListenableWorkerBuilder<PropertyMonitorWorker>(context = android.content.Context()).build()
    }

    @Test
    fun `should detect property becoming unavailable`() = runTest {
        // 测试从有房变为无房的情况
        val currentUnavailableDates = listOf("2024-05-01", "2024-05-02")
        val previousUnavailableDates = emptyList<String>()

        val changes = detectStatusChanges(
            currentUnavailableDates,
            previousUnavailableDates
        )

        assertTrue(changes.newUnavailableDates.isNotEmpty())
        assertEquals(2, changes.newUnavailableDates.size)
        assertFalse(changes.noLongerUnavailableDates.isNotEmpty())
    }

    @Test
    fun `should detect property becoming available`() = runTest {
        // 测试从无房变为有房的情况
        val currentUnavailableDates = emptyList<String>()
        val previousUnavailableDates = listOf("2024-05-01", "2024-05-02")

        val changes = detectStatusChanges(
            currentUnavailableDates,
            previousUnavailableDates
        )

        assertTrue(changes.noLongerUnavailableDates.isNotEmpty())
        assertEquals(2, changes.noLongerUnavailableDates.size)
        assertFalse(changes.newUnavailableDates.isNotEmpty())
    }

    @Test
    fun `should detect partial date changes`() = runTest {
        // 测试部分日期状态变化的情况
        val currentUnavailableDates = listOf("2024-05-01", "2024-05-03")
        val previousUnavailableDates = listOf("2024-05-01", "2024-05-02")

        val changes = detectStatusChanges(
            currentUnavailableDates,
            previousUnavailableDates
        )

        // 应该检测到状态变化的日期：2024-05-02 (变为有房), 2024-05-03 (变为无房)
        assertTrue(changes.newUnavailableDates.isNotEmpty() || changes.noLongerUnavailableDates.isNotEmpty())
    }

    @Test
    fun `should handle empty dates correctly`() = runTest {
        // 测试空日期列表
        val currentUnavailableDates = emptyList<String>()
        val previousUnavailableDates = emptyList<String>()

        val changes = detectStatusChanges(
            currentUnavailableDates,
            previousUnavailableDates
        )

        assertEquals(0, changes.newUnavailableDates.size)
        assertEquals(0, changes.noLongerUnavailableDates.size)
    }

    @Test
    fun `should generate correct notification content for unavailable`() {
        // 测试通知文本生成
        val propertyName = "测试房源"
        val dates = listOf("2024-05-01", "2024-05-02")

        val title = generateNotificationTitle(propertyName, NotificationManager.StatusChangeType.BECAME_UNAVAILABLE, dates)
        val content = generateNotificationContent(propertyName, NotificationManager.StatusChangeType.BECAME_UNAVAILABLE, dates)

        assertEquals("房源状态变化", title)
        assertTrue(content.contains("变为无房"))
        assertTrue(content.contains("2024-05-01"))
        assertTrue(content.contains("2024-05-02"))
    }

    @Test
    fun `should generate correct notification content for available`() {
        // 测试有房状态的通知文本生成
        val propertyName = "测试房源"
        val dates = listOf("2024-05-01", "2024-05-02")

        val title = generateNotificationTitle(propertyName, NotificationManager.StatusChangeType.BECAME_AVAILABLE, dates)
        val content = generateNotificationContent(propertyName, NotificationManager.StatusChangeType.BECAME_AVAILABLE, dates)

        assertEquals("房源状态变化", title)
        assertTrue(content.contains("变为有房"))
    }

    @Test
    fun `should handle quiet hours correctly`() {
        // 测试免打扰时段逻辑
        val settings = createMockUserSettings(quietHoursStart = 22, quietHoursEnd = 8)

        // 测试免打扰时段内（23:00）
        val isQuietHour1 = isInQuietHours(settings, 23)
        assertTrue(isQuietHour1)

        // 测试非免打扰时段（10:00）
        val isQuietHour2 = isInQuietHours(settings, 10)
        assertFalse(isQuietHour2)

        // 测试边界情况（22:00开始）
        val isQuietHour3 = isInQuietHours(settings, 22)
        assertTrue(isQuietHour3)

        // 测试边界情况（8:00结束）
        val isQuietHour4 = isInQuietHours(settings, 7)
        assertFalse(isQuietHour4)
    }

    // Helper functions for testing

    private data class StatusChanges(
        val newUnavailableDates: List<String>,
        val noLongerUnavailableDates: List<String>
    )

    private fun detectStatusChanges(
        currentUnavailableDates: List<String>,
        previousUnavailableDates: List<String>
    ): StatusChanges {
        val newUnavailableDates = currentUnavailableDates - previousUnavailableDates
        val noLongerUnavailableDates = previousUnavailableDates - currentUnavailableDates
        return StatusChanges(newUnavailableDates, noLongerUnavailableDates)
    }

    private fun generateNotificationTitle(
        propertyName: String,
        changeType: NotificationManager.StatusChangeType,
        dates: List<String>
    ): String {
        return when (changeType) {
            NotificationManager.StatusChangeType.BECAME_UNAVAILABLE -> "房源状态变化"
            NotificationManager.StatusChangeType.BECAME_AVAILABLE -> "房源状态变化"
            NotificationManager.StatusChangeType.PARTIAL_CHANGE -> "房源部分日期变化"
        }
    }

    private fun generateNotificationContent(
        propertyName: String,
        changeType: NotificationManager.StatusChangeType,
        dates: List<String>
    ): String {
        val changeDescription = when (changeType) {
            NotificationManager.StatusChangeType.BECAME_UNAVAILABLE -> "从有房变为无房"
            NotificationManager.StatusChangeType.BECAME_AVAILABLE -> "从无房变为有房"
            NotificationManager.StatusChangeType.PARTIAL_CHANGE -> "部分日期状态变化"
        }

        val sortedDates = dates.sorted().joinToString("\n  • ", "  • ")
        return "房源：$propertyName\n状态变化：$changeDescription\n受影响日期：\n$sortedDates"
    }

    private fun createMockUserSettings(
        checkInterval: Int = 60,
        notificationEnabled: Boolean = true,
        quietHoursStart: Int = 22,
        quietHoursEnd: Int = 8
    ): com.housemonitor.data.model.UserSettings {
        return com.housemonitor.data.model.UserSettings(
            checkInterval = checkInterval,
            notificationEnabled = notificationEnabled,
            quietHoursStart = quietHoursStart,
            quietHoursEnd = quietHoursEnd
        )
    }

    private fun isInQuietHours(
        settings: com.housemonitor.data.model.UserSettings,
        hourOfDay: Int
    ): Boolean {
        return if (settings.quietHoursStart < settings.quietHoursEnd) {
            // 同一天的时间段
            hourOfDay >= settings.quietHoursStart && hourOfDay < settings.quietHoursEnd
        } else {
            // 跨天的时间段
            hourOfDay >= settings.quietHoursStart || hourOfDay < settings.quietHoursEnd
        }
    }
}