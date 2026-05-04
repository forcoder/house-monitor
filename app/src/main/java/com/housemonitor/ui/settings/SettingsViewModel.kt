package com.housemonitor.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.housemonitor.data.model.OtaUpdate
import com.housemonitor.data.model.UpdateStatus
import com.housemonitor.data.repository.UserSettingsRepository
import com.housemonitor.service.OtaManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
    private val otaManager: OtaManager,
    @ApplicationContext private val application: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // OTA 更新状态（直接暴露 OtaManager 的 StateFlow）
    val updateStatus: StateFlow<UpdateStatus> = otaManager.updateStatus
    val availableUpdate: StateFlow<OtaUpdate?> = otaManager.availableUpdate
    val downloadProgress: StateFlow<Float> = otaManager.downloadProgress
    val errorMessage: StateFlow<String?> = otaManager.errorMessage

    init {
        loadSettings()

        // 自动检查：进入设置页时，距上次检查超过1小时则自动检查
        viewModelScope.launch {
            val prefs = application.getSharedPreferences("ota_auto_check", Context.MODE_PRIVATE)
            val lastCheckTime = prefs.getLong("last_settings_check", 0)
            val oneHourAgo = System.currentTimeMillis() - 3600_000
            if (lastCheckTime < oneHourAgo) {
                otaManager.checkForUpdate()
                prefs.edit().putLong("last_settings_check", System.currentTimeMillis()).apply()
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            userSettingsRepository.getUserSettings().collect { settings ->
                _uiState.value = _uiState.value.copy(
                    userSettings = settings,
                    isLoading = false
                )
            }
        }
    }

    fun updateCheckInterval(interval: Int) {
        viewModelScope.launch {
            val result = userSettingsRepository.updateCheckInterval(interval)
            result.fold(
                onSuccess = { },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "更新检查间隔失败: ${it.message}"
                    )
                }
            )
        }
    }

    fun toggleNotifications() {
        viewModelScope.launch {
            val currentSettings = _uiState.value.userSettings
            currentSettings?.let {
                val result = userSettingsRepository.updateNotificationEnabled(!it.notificationEnabled)
                result.fold(
                    onSuccess = { },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "更新通知设置失败: ${it.message}"
                        )
                    }
                )
            }
        }
    }

    fun toggleAutoRefresh() {
        // TODO: 实现自动刷新切换
    }

    fun updateQuietHours(startHour: Int, endHour: Int) {
        viewModelScope.launch {
            val result = userSettingsRepository.updateQuietHours(startHour, endHour)
            result.fold(
                onSuccess = { },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "更新免打扰时段失败: ${it.message}"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // ========== OTA 更新 ==========

    fun checkForUpdate() {
        viewModelScope.launch {
            otaManager.checkForUpdate()
        }
    }

    fun startDownload() {
        val update = otaManager.availableUpdate.value
        if (update != null) {
            otaManager.startDownload(update)
        }
    }

    fun triggerInstall() {
        otaManager.triggerInstall()
    }

    fun cancelDownload() {
        otaManager.cancelDownload()
    }

    fun clearUpdateState() {
        otaManager.cleanup()
    }
}

data class SettingsUiState(
    val userSettings: com.housemonitor.data.model.UserSettings? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)