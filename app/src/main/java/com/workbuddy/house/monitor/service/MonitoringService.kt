package com.workbuddy.house.monitor.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.workbuddy.house.monitor.R
import com.workbuddy.house.monitor.model.AvailabilityResult
import kotlinx.coroutines.*

class MonitoringService : Service() {

    private val mtMonitor = MtMeituanMonitor()
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var monitoringJob: Job? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "monitoring_channel"
        private const val TAG = "MonitoringService"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("房源监控服务运行中"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_MONITORING" -> {
                val meituanUrl = intent.getStringExtra("MEITUAN_URL")
                if (meituanUrl != null) {
                    startMonitoring(meituanUrl)
                }
            }
            "STOP_MONITORING" -> {
                stopMonitoring()
            }
        }
        return START_STICKY
    }

    private fun startMonitoring(meituanUrl: String) {
        stopMonitoring() // 停止之前的监控任务

        monitoringJob = serviceScope.launch {
            while (isActive) {
                try {
                    Log.d(TAG, "开始检查房源可用性")
                    mtMonitor.checkAvailability(meituanUrl) { result ->
                        result.fold(
                            onSuccess = { availabilityResult ->
                                handleAvailabilityResult(availabilityResult)
                            },
                            onFailure = { error ->
                                Log.e(TAG, "监控检查失败", error)
                            }
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "监控过程中出现异常", e)
                }

                delay(5 * 60 * 1000) // 每5分钟检查一次
            }
        }

        Log.d(TAG, "房源监控已启动")
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        Log.d(TAG, "房源监控已停止")
    }

    private fun handleAvailabilityResult(result: AvailabilityResult) {
        if (result.unavailableDates.isNotEmpty()) {
            val unavailableDates = result.unavailableDates.joinToString(", ") { it.date }
            showNotification(
                "发现无房日期",
                "以下日期无房: $unavailableDates"
            )
            Log.d(TAG, "发现无房日期: $unavailableDates")
        } else {
            Log.d(TAG, "所有日期都有房")
        }
    }

    private fun showNotification(title: String, content: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            try {
                notify(NOTIFICATION_ID + 1, builder.build())
            } catch (e: SecurityException) {
                Log.e(TAG, "发送通知失败", e)
            }
        }
    }

    private fun createNotification(content: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("房源监控")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
    }

    private fun createNotificationChannel() {
        // 创建通知渠道（Android 8.0+）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "房源监控",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "房源可用性监控通知"
            }

            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "监控服务已销毁")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}