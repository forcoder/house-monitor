package com.workbuddy.house.monitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.workbuddy.house.monitor.service.MonitoringService
import com.workbuddy.house.monitor.util.PreferencesHelper

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "设备重启，检查是否需要重启监控服务")

            val prefs = PreferencesHelper(context)
            val isMonitoring = prefs.isMonitoringEnabled()
            val meituanUrl = prefs.getMeituanUrl()

            if (isMonitoring && meituanUrl.isNotEmpty()) {
                Log.d("BootReceiver", "自动重启监控服务")
                val serviceIntent = Intent(context, MonitoringService::class.java).apply {
                    action = "START_MONITORING"
                    putExtra("MEITUAN_URL", meituanUrl)
                }
                context.startService(serviceIntent)
            }
        }
    }
}