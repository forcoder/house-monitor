package com.workbuddy.house.monitor.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.workbuddy.house.monitor.service.MonitoringService
import com.workbuddy.house.monitor.util.NotificationHelper
import com.workbuddy.house.monitor.util.PreferencesHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesHelper(application)
    private val notificationHelper = NotificationHelper(application)

    private val _monitoringStatus = MutableLiveData<Boolean>(false)
    val monitoringStatus: LiveData<Boolean> = _monitoringStatus

    private val _lastCheckTime = MutableLiveData<String>("")
    val lastCheckTime: LiveData<String> = _lastCheckTime

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadMonitoringStatus()
    }

    private fun loadMonitoringStatus() {
        val isMonitoring = prefs.isMonitoringEnabled()
        _monitoringStatus.value = isMonitoring

        val lastCheck = prefs.getLastCheckTime()
        if (lastCheck > 0) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            _lastCheckTime.value = dateFormat.format(Date(lastCheck))
        }
    }

    fun startMonitoring(meituanUrl: String) {
        viewModelScope.launch {
            try {
                // 保存配置
                prefs.setMeituanUrl(meituanUrl)
                prefs.setMonitoringEnabled(true)

                // 启动监控服务
                val serviceIntent = Intent(getApplication(), MonitoringService::class.java).apply {
                    action = "START_MONITORING"
                    putExtra("MEITUAN_URL", meituanUrl)
                }
                getApplication<Application>().startService(serviceIntent)

                _monitoringStatus.value = true
                _errorMessage.value = null

                // 发送状态通知
                notificationHelper.showMonitoringStatusNotification(true)

                updateLastCheckTime()
            } catch (e: Exception) {
                _errorMessage.value = "启动监控失败: ${e.message}"
            }
        }
    }

    fun stopMonitoring() {
        viewModelScope.launch {
            try {
                // 停止监控服务
                val serviceIntent = Intent(getApplication(), MonitoringService::class.java).apply {
                    action = "STOP_MONITORING"
                }
                getApplication<Application>().startService(serviceIntent)

                // 更新状态
                prefs.setMonitoringEnabled(false)
                _monitoringStatus.value = false

                // 发送状态通知
                notificationHelper.showMonitoringStatusNotification(false)

            } catch (e: Exception) {
                _errorMessage.value = "停止监控失败: ${e.message}"
            }
        }
    }

    fun updateLastCheckTime() {
        val currentTime = System.currentTimeMillis()
        prefs.setLastCheckTime(currentTime)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        _lastCheckTime.value = dateFormat.format(Date(currentTime))
    }

    fun clearError() {
        _errorMessage.value = null
    }
}