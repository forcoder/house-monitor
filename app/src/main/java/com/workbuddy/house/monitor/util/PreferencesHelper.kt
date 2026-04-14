package com.workbuddy.house.monitor.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "house_monitor_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_MEITUAN_URL = "meituan_url"
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
        private const val KEY_LAST_CHECK_TIME = "last_check_time"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
    }

    fun setMeituanUrl(url: String) {
        prefs.edit().putString(KEY_MEITUAN_URL, url).apply()
    }

    fun getMeituanUrl(): String {
        return prefs.getString(KEY_MEITUAN_URL, "") ?: ""
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MONITORING_ENABLED, enabled).apply()
    }

    fun isMonitoringEnabled(): Boolean {
        return prefs.getBoolean(KEY_MONITORING_ENABLED, false)
    }

    fun setLastCheckTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_CHECK_TIME, time).apply()
    }

    fun getLastCheckTime(): Long {
        return prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
    }

    fun setNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
    }

    fun isNotificationEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)
    }
}