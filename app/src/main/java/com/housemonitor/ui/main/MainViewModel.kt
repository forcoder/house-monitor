package com.housemonitor.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.housemonitor.data.repository.PropertyRepository
import com.housemonitor.data.repository.UserSettingsRepository
import com.housemonitor.service.WorkManagerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val workManagerService: WorkManagerService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    init {
        loadData()
        initializeSettings()
    }

    private fun loadData() {
        viewModelScope.launch {
            // 加载房源列表
            propertyRepository.getAllProperties().collect { properties ->
                _uiState.value = _uiState.value.copy(
                    properties = properties,
                    isLoading = false
                )
            }
        }

        viewModelScope.launch {
            // 加载用户设置
            userSettingsRepository.getUserSettings().collect { settings ->
                _uiState.value = _uiState.value.copy(
                    userSettings = settings
                )
            }
        }
    }

    private fun initializeSettings() {
        viewModelScope.launch {
            userSettingsRepository.initializeDefaultSettings()
            workManagerService.schedulePeriodicMonitoring()
        }
    }

    fun addProperty(name: String, url: String, description: String = "", platform: String = "meituan") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = propertyRepository.addProperty(name, url, description, platform)
            result.fold(
                onSuccess = {
                    // 成功添加后立即开始监控
                    workManagerService.scheduleImmediateMonitoring(it.id)
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "添加房源失败: ${it.message}",
                        isLoading = false
                    )
                }
            )
        }
    }

    fun removeProperty(propertyId: String) {
        viewModelScope.launch {
            val result = propertyRepository.removeProperty(propertyId)
            result.fold(
                onSuccess = {
                    // 重新安排监控
                    workManagerService.schedulePeriodicMonitoring()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "删除房源失败: ${it.message}"
                    )
                }
            )
        }
    }

    fun togglePropertyActive(propertyId: String) {
        viewModelScope.launch {
            propertyRepository.updatePropertyActive(propertyId, true)
            // 重新安排监控
            workManagerService.schedulePeriodicMonitoring()
        }
    }

    fun refreshAllProperties() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            workManagerService.scheduleImmediateMonitoring()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun updateCheckInterval(interval: Int) {
        viewModelScope.launch {
            val result = userSettingsRepository.updateCheckInterval(interval)
            result.fold(
                onSuccess = {
                    workManagerService.updateMonitoringInterval()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "更新检查间隔失败: ${it.message}"
                    )
                }
            )
        }
    }

    fun updateNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val result = userSettingsRepository.updateNotificationEnabled(enabled)
            result.fold(
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "更新通知设置失败: ${it.message}"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun navigateToProperty(propertyName: String?) {
        viewModelScope.launch {
            propertyName?.let {
                _navigationEvent.emit("property_detail/$it")
            }
        }
    }
}

data class MainUiState(
    val properties: List<com.housemonitor.data.model.Property> = emptyList(),
    val userSettings: com.housemonitor.data.model.UserSettings? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)