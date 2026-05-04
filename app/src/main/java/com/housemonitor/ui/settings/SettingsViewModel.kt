package com.housemonitor.ui.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.housemonitor.data.model.OtaUpdate
import com.housemonitor.data.model.Property
import com.housemonitor.data.model.UpdateStatus
import com.housemonitor.data.repository.PropertyRepository
import com.housemonitor.data.repository.UserSettingsRepository
import com.housemonitor.service.OtaManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
    private val propertyRepository: PropertyRepository,
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

    // 导入/导出消息
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

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

    fun clearSnackbar() {
        _snackbarMessage.value = null
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

    // ========== 数据备份/恢复 ==========

    /**
     * 导出房源配置为 JSON 文本，返回 JSON 字符串
     */
    suspend fun exportPropertiesToJson(): String? {
        return try {
            val properties = propertyRepository.getAllProperties().first()
            val backupData = mapOf(
                "version" to 1,
                "exportTime" to System.currentTimeMillis(),
                "appVersion" to com.housemonitor.BuildConfig.VERSION_NAME,
                "properties" to properties.map { p ->
                    mapOf(
                        "name" to p.name,
                        "url" to p.url,
                        "platform" to p.platform,
                        "description" to p.description,
                        "isActive" to p.isActive
                    )
                }
            )
            com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(backupData)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从 JSON 文本恢复房源配置
     * @return 成功导入的房源数量，-1 表示失败
     */
    suspend fun importPropertiesFromJson(json: String): Int {
        return try {
            val backup = com.google.gson.JsonParser.parseString(json).asJsonObject
            val propertiesArray = backup.getAsJsonArray("properties") ?: return -1
            val listType = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
            val properties: List<Map<String, Any>> = com.google.gson.Gson().fromJson(propertiesArray, listType)

            var count = 0
            val existing = propertyRepository.getAllProperties().first()
            properties.forEach { p ->
                try {
                    val name = p["name"] as? String ?: return@forEach
                    val url = p["url"] as? String ?: return@forEach
                    val platform = p["platform"] as? String ?: "meituan"
                    val description = p["description"] as? String ?: ""
                    val isActive = (p["isActive"] as? Boolean) ?: true

                    if (existing.none { it.url == url }) {
                        val result = propertyRepository.addProperty(name, url, description, platform)
                        result.onSuccess { newProperty ->
                            if (!isActive) {
                                propertyRepository.updatePropertyActive(newProperty.id, false)
                            }
                        }
                        if (result.isSuccess) count++
                    }
                } catch (_: Exception) { }
            }
            // 重新加载设置
            loadSettings()
            count
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * 分享导出的数据（通过系统分享）
     */
    fun shareBackupText(text: String) {
        try {
            // 先写入临时文件
            val backupFile = File(application.cacheDir, "house-monitor-backup.json")
            backupFile.writeText(text)

            val uri = FileProvider.getUriForFile(
                application,
                "${application.packageName}.fileprovider",
                backupFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "房源监控数据备份")
                putExtra(Intent.EXTRA_TEXT, "房源监控数据备份文件，重装应用后可导入恢复。")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "导出数据备份")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            application.startActivity(chooser)
        } catch (e: Exception) {
            _snackbarMessage.value = "导出失败: ${e.message}"
        }
    }
}

data class SettingsUiState(
    val userSettings: com.housemonitor.data.model.UserSettings? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
