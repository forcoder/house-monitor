package com.workbuddy.house.monitor.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.workbuddy.house.monitor.R

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "house_monitor_channel"
        private const val CHANNEL_NAME = "房源监控通知"
        private const val CHANNEL_DESCRIPTION = "美团小程序房源监控通知"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showAvailabilityNotification(unavailableDates: List<String>) {
        if (unavailableDates.isEmpty()) return

        val title = "🏠 发现无房日期"
        val content = buildNotificationContent(unavailableDates)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content.first)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.second))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setLights(0xFF00FF00.toInt(), 300, 1000)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        }
    }

    private fun buildNotificationContent(unavailableDates: List<String>): Pair<String, String> {
        val shortContent = if (unavailableDates.size <= 3) {
            "无房日期: ${unavailableDates.joinToString(", ")}"
        } else {
            "发现${unavailableDates.size}个无房日期，点击查看详细信息"
        }

        val fullContent = """检测到以下日期无房:
            |
            |${unavailableDates.joinToString("\n")}
            |
            |请及时关注房源变化，祝您租房顺利！
        """.trimMargin()

        return Pair(shortContent, fullContent)
    }

    fun showMonitoringStatusNotification(isRunning: Boolean) {
        val title = if (isRunning) "监控已启动" else "监控已停止"
        val content = if (isRunning) {
            "房源监控服务正在运行中，将每5分钟检查一次房源状态"
        } else {
            "房源监控服务已停止"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(1000, builder.build())
            }
        }
    }
}