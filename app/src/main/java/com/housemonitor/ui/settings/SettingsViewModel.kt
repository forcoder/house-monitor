package com.housemonitor.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.housemonitor.data.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
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
}

data class SettingsUiState(
    val userSettings: com.housemonitor.data.model.UserSettings? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)