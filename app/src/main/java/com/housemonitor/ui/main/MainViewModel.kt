package com.housemonitor.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.housemonitor.data.model.MonitorRecord
import com.housemonitor.data.repository.MonitorRepository
import com.housemonitor.data.repository.PropertyRepository
import com.housemonitor.data.repository.UserSettingsRepository
import com.housemonitor.service.MonitorUseCase
import com.housemonitor.service.WorkManagerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val workManagerService: WorkManagerService,
    private val monitorRepository: MonitorRepository,
    private val monitorUseCase: MonitorUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    // 正在检查的房源 ID 集合
    private val _checkingIds = MutableStateFlow<Set<String>>(emptySet())
    val checkingIds: StateFlow<Set<String>> = _checkingIds.asStateFlow()

    init {
        loadData()
        initializeSettings()
    }

    private fun loadData() {
        viewModelScope.launch {
            propertyRepository.getAllProperties().collect { properties ->
                val latestRecords = mutableMapOf<String, MonitorRecord?>()
                for (property in properties) {
                    latestRecords[property.id] = monitorRepository.getLastSuccessRecord(property.id)
                }
                _uiState.value = _uiState.value.copy(
                    properties = properties,
                    latestRecords = latestRecords,
                    isLoading = false
                )
            }
        }

        viewModelScope.launch {
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
            val current = _uiState.value.properties.find { it.id == propertyId }
            val newActive = current?.isActive != true
            propertyRepository.updatePropertyActive(propertyId, newActive)
            workManagerService.schedulePeriodicMonitoring()
            val updatedProperties = _uiState.value.properties.map {
                if (it.id == propertyId) it.copy(isActive = newActive) else it
            }
            _uiState.value = _uiState.value.copy(properties = updatedProperties)
        }
    }

    /**
     * 同步检查单个房源 — 立即执行，完成后更新 UI
     */
    fun refreshSingleProperty(propertyId: String) {
        val property = _uiState.value.properties.find { it.id == propertyId } ?: return
        if (_checkingIds.value.contains(propertyId)) return // 已经在检查中

        viewModelScope.launch {
            _checkingIds.value = _checkingIds.value + propertyId
            try {
                val result = monitorUseCase.execute(property)
                if (result != null) {
                    // 更新该房源的最新记录
                    val updatedRecords = _uiState.value.latestRecords.toMutableMap()
                    updatedRecords[propertyId] = result.record
                    // 更新房源的 lastCheckedAt
                        val updatedProperties = _uiState.value.properties.map {
                        if (it.id == propertyId) it.copy(lastCheckedAt = System.currentTimeMillis()) else it
                    }
                    _uiState.value = _uiState.value.copy(
                        latestRecords = updatedRecords,
                        properties = updatedProperties
                    )
                }
            } finally {
                _checkingIds.value = _checkingIds.value - propertyId
            }
        }
    }

    /**
     * 同步检查所有活跃房源 — 逐个执行，每个完成后立即更新 UI
     */
    fun refreshAllProperties() {
        val activeProperties = _uiState.value.properties.filter { it.isActive }
        if (activeProperties.isEmpty()) return
        if (_checkingIds.value.isNotEmpty()) return // 已有检查在进行中

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)

            activeProperties.forEach { property ->
                _checkingIds.value = _checkingIds.value + property.id
                try {
                    val result = monitorUseCase.execute(property)
                    if (result != null) {
                        val updatedRecords = _uiState.value.latestRecords.toMutableMap()
                        updatedRecords[property.id] = result.record
                        val updatedProperties = _uiState.value.properties.map {
                            if (it.id == property.id) it.copy(lastCheckedAt = System.currentTimeMillis()) else it
                        }
                        _uiState.value = _uiState.value.copy(
                            latestRecords = updatedRecords,
                            properties = updatedProperties
                        )
                    }
                } finally {
                    _checkingIds.value = _checkingIds.value - property.id
                }
            }

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
                onSuccess = { },
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
    val errorMessage: String? = null,
    val latestRecords: Map<String, MonitorRecord?> = emptyMap()
)
