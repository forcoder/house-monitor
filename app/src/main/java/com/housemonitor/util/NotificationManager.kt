package com.housemonitor.util

import android.app.NotificationChannel
import android.app.NotificationManager as SystemNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.housemonitor.R
import com.housemonitor.ui.history.HistoryActivity
import com.housemonitor.ui.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as SystemNotificationManager

    companion object {
        private const val CHANNEL_ID = "property_monitor_channel"
        private const val CHANNEL_NAME = "房源监控通知"
        private const val CHANNEL_DESCRIPTION = "房源无房日期提醒"
        private const val GROUP_KEY_PROPERTY_NOTIFICATIONS = "group_property_notifications"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                SystemNotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showUnavailableDatesNotification(
        propertyName: String,
        unavailableDates: List<String>
    ) {
        if (unavailableDates.isEmpty()) return

        val notificationId = Random.nextInt(1000, 9999)
        val summaryNotificationId = 1000 // 固定ID用于分组通知

        // 创建点击通知时打开的Intent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "unavailable_dates")
            putExtra("property_name", propertyName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 创建单个通知
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("房源无房提醒")
            .setContentText(generateNotificationText(propertyName, unavailableDates))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(generateDetailedNotificationText(propertyName, unavailableDates))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_PROPERTY_NOTIFICATIONS)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .addAction(
                R.drawable.ic_check,
                "查看详情",
                pendingIntent
            )
            .build()

        notificationManager.notify(notificationId, notification)

        // 创建分组摘要通知
        createGroupSummaryNotification(propertyName, unavailableDates.size)
    }

    /**
     * 显示状态变化通知（所有类型）
     */
    fun showStatusChangeNotification(
        propertyName: String,
        changeType: StatusChangeType,
        dates: List<String>
    ) {
        if (dates.isEmpty()) return

        val notificationId = Random.nextInt(1000, 9999)
        val title = when (changeType) {
            StatusChangeType.BECAME_UNAVAILABLE -> "房源状态变化"
            StatusChangeType.BECAME_AVAILABLE -> "房源状态变化"
            StatusChangeType.PARTIAL_CHANGE -> "房源部分日期变化"
        }

        // 创建点击通知时打开历史记录的Intent
        val intent = Intent(context, HistoryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_property", propertyName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 生成通知文本
        val contentText = generateStatusChangeText(propertyName, changeType, dates)
        val detailedText = generateDetailedStatusChangeText(propertyName, changeType, dates)

        // 创建单个通知
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(detailedText)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_PROPERTY_NOTIFICATIONS)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .addAction(
                R.drawable.ic_history,
                "查看历史",
                pendingIntent
            )
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * 状态变化类型枚举
     */
    enum class StatusChangeType {
        BECAME_UNAVAILABLE,  // 从有房变为无房
        BECAME_AVAILABLE,    // 从无房变为有房
        PARTIAL_CHANGE       // 部分日期状态变化
    }

    private fun generateNotificationText(propertyName: String, dates: List<String>): String {
        return when {
            dates.size == 1 -> "$propertyName 在 ${dates.first()} 无房"
            dates.size <= 3 -> "$propertyName 在 ${dates.size} 个日期无房"
            else -> "$propertyName 在 ${dates.size} 个日期无房，点击查看详情"
        }
    }

    private fun generateDetailedNotificationText(propertyName: String, dates: List<String>): String {
        val sortedDates = dates.sorted()
        val datesText = when {
            dates.size <= 5 -> sortedDates.joinToString("\n  • ", "  • ")
            else -> {
                val firstThree = sortedDates.take(3).joinToString("\n  • ", "  • ")
                val remaining = sortedDates.size - 3
                "$firstThree\n  • ...还有 $remaining 个日期"
            }
        }

        return "房源：$propertyName\n无房日期：\n$datesText"
    }

    /**
     * 生成状态变化通知文本
     */
    private fun generateStatusChangeText(propertyName: String, changeType: StatusChangeType, dates: List<String>): String {
        val dateCount = dates.size
        return when (changeType) {
            StatusChangeType.BECAME_UNAVAILABLE -> {
                if (dateCount == 1) "$propertyName 在 ${dates.first()} 变为无房"
                else "$propertyName 在 $dateCount 个日期变为无房"
            }
            StatusChangeType.BECAME_AVAILABLE -> {
                if (dateCount == 1) "$propertyName 在 ${dates.first()} 变为有房"
                else "$propertyName 在 $dateCount 个日期变为有房"
            }
            StatusChangeType.PARTIAL_CHANGE -> {
                if (dateCount <= 3) "$propertyName 在 $dateCount 个日期状态变化"
                else "$propertyName 在 $dateCount 个日期状态变化，点击查看详情"
            }
        }
    }

    /**
     * 生成详细的状态变化通知文本
     */
    private fun generateDetailedStatusChangeText(propertyName: String, changeType: StatusChangeType, dates: List<String>): String {
        val sortedDates = dates.sorted()
        val changeDescription = when (changeType) {
            StatusChangeType.BECAME_UNAVAILABLE -> "从有房变为无房"
            StatusChangeType.BECAME_AVAILABLE -> "从无房变为有房"
            StatusChangeType.PARTIAL_CHANGE -> "部分日期状态变化"
        }

        val datesText = when {
            dates.size <= 5 -> sortedDates.joinToString("\n  • ", "  • ")
            else -> {
                val firstThree = sortedDates.take(3).joinToString("\n  • ", "  • ")
                val remaining = dates.size - 3
                "$firstThree\n  • ...还有 $remaining 个日期"
            }
        }

        return "房源：$propertyName\n状态变化：$changeDescription\n受影响日期：\n$datesText"
    }

    private fun createGroupSummaryNotification(lastPropertyName: String, totalNotifications: Int) {
        if (totalNotifications <= 1) return

        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("房源监控摘要")
            .setContentText("发现多个房源无房日期")
            .setStyle(NotificationCompat.InboxStyle()
                .addLine("最新：$lastPropertyName 无房提醒")
                .addLine("共 $totalNotifications 个通知")
                .setSummaryText("房源监控")
            )
            .setGroup(GROUP_KEY_PROPERTY_NOTIFICATIONS)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1000, summaryNotification)
    }

    fun showMonitoringStartedNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("房源监控已启动")
            .setContentText("正在监控您添加的房源日历")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(9999, notification)
    }

    fun showMonitoringErrorNotification(errorMessage: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_error)
            .setContentTitle("监控出错")
            .setContentText(errorMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Random.nextInt(2000, 2999), notification)
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            channel?.importance != SystemNotificationManager.IMPORTANCE_NONE
        } else {
            notificationManager.areNotificationsEnabled()
        }
    }
}