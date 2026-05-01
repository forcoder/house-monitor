package com.housemonitor.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.housemonitor.data.model.MonitorRecord
import com.housemonitor.data.model.Property
import com.housemonitor.data.repository.MonitorRepository
import com.housemonitor.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val monitorRepository: MonitorRepository,
    private val propertyRepository: PropertyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // 加载所有房源列表
            propertyRepository.getAllProperties().collect { properties ->
                _uiState.value = _uiState.value.copy(allProperties = properties)
            }
        }

        viewModelScope.launch {
            // 加载监控记录
            monitorRepository.getRecentRecords(100).collect { records ->
                _uiState.value = _uiState.value.copy(allRecords = records)
            }
        }
    }

    /**
     * 按房源筛选记录
     */
    fun filterByProperty(propertyId: String) {
        viewModelScope.launch {
            val propertyName = _uiState.value.allProperties.find { it.id == propertyId }?.name ?: "未知房源"

            val filteredRecords = _uiState.value.allRecords.filter { record ->
                record.propertyId == propertyId
            }.sortedByDescending { it.checkedAt }

            _uiState.value = _uiState.value.copy(
                selectedPropertyId = propertyId,
                propertyName = propertyName,
                filteredRecords = filteredRecords
            )
        }
    }

    /**
     * 清除筛选条件
     */
    fun clearFilter() {
        _uiState.value = _uiState.value.copy(
            selectedPropertyId = null,
            propertyName = null,
            filteredRecords = emptyList()
        )
    }

    /**
     * 刷新数据
     */
    fun refreshData() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                // 触发重新加载
                loadData()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "刷新失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 清理旧记录
     */
    fun cleanupOldRecords(days: Int = 30) {
        viewModelScope.launch {
            try {
                monitorRepository.cleanupOldRecords(days)
                // 重新加载数据
                loadData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "清理失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * 获取指定日期范围内的记录
     */
    fun getRecordsForDateRange(startDate: String, endDate: String): List<MonitorRecord> {
        return _uiState.value.allRecords.filter { record ->
            record.checkDate in startDate..endDate
        }.sortedByDescending { it.checkedAt }
    }
}

data class HistoryUiState(
    val allProperties: List<Property> = emptyList(),
    val allRecords: List<MonitorRecord> = emptyList(),
    val filteredRecords: List<MonitorRecord> = emptyList(),
    val selectedPropertyId: String? = null,
    val propertyName: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)