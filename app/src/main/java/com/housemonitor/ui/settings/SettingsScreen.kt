package com.housemonitor.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.housemonitor.BuildConfig
import com.housemonitor.R
import com.housemonitor.data.model.UpdateStatus
import com.housemonitor.ui.settings.DownloadProgressDialog
import com.housemonitor.ui.settings.InstallPromptDialog
import com.housemonitor.ui.settings.SettingsViewModel
import com.housemonitor.ui.settings.UpdateDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // OTA 状态
    val updateStatus by viewModel.updateStatus.collectAsState()
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    // Dialog 显示控制
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showInstallDialog by remember { mutableStateOf(false) }
    var showIntervalDialog by remember { mutableStateOf(false) }

    // 根据状态自动显示弹窗
    LaunchedEffect(updateStatus) {
        when (updateStatus) {
            UpdateStatus.UPDATE_AVAILABLE -> showUpdateDialog = true
            UpdateStatus.DOWNLOADING -> showDownloadDialog = true
            UpdateStatus.DOWNLOADED -> {
                showDownloadDialog = false
                showInstallDialog = true
            }
            UpdateStatus.FAILED -> {
                showDownloadDialog = false
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 检查更新 — 放在最顶部，独立醒目区域
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Surface(
                    onClick = { viewModel.checkForUpdate() },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.01f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Update,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.check_for_update),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (updateStatus) {
                                    UpdateStatus.CHECKING -> stringResource(R.string.checking_update)
                                    UpdateStatus.DOWNLOADING -> "${stringResource(R.string.downloading_update)} ${(downloadProgress * 100).toInt()}%"
                                    UpdateStatus.UPDATE_AVAILABLE -> "${stringResource(R.string.update_available)}：${availableUpdate?.versionName ?: ""}"
                                    UpdateStatus.DOWNLOADED -> stringResource(R.string.download_complete)
                                    UpdateStatus.FAILED -> "检查失败，请重试"
                                    else -> "当前版本 ${BuildConfig.VERSION_NAME}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (updateStatus == UpdateStatus.CHECKING) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else if (updateStatus == UpdateStatus.UPDATE_AVAILABLE) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(
                                        MaterialTheme.colorScheme.error,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "!",
                                    color = MaterialTheme.colorScheme.onError,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        } else {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 监控设置
            SettingsSection(
                title = "监控设置",
                icon = Icons.Default.Info
            ) {
                SettingsItem(
                    title = "检查间隔",
                    subtitle = "${uiState.userSettings?.checkInterval ?: 5} 分钟",
                    onClick = { showIntervalDialog = true }
                )

                SettingsItem(
                    title = "自动刷新",
                    subtitle = if (uiState.userSettings?.autoRefreshEnabled == true) "已开启" else "已关闭",
                    onClick = {
                        viewModel.toggleAutoRefresh()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 通知设置
            SettingsSection(
                title = "通知设置",
                icon = Icons.Default.Notifications
            ) {
                SettingsItem(
                    title = "推送通知",
                    subtitle = if (uiState.userSettings?.notificationEnabled == true) "已开启" else "已关闭",
                    onClick = {
                        viewModel.toggleNotifications()
                    }
                )

                SettingsItem(
                    title = "免打扰时段",
                    subtitle = "${uiState.userSettings?.quietHoursStart ?: 22}:00 - ${uiState.userSettings?.quietHoursEnd ?: 8}:00",
                    onClick = {
                        // 显示时间设置对话框
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 数据管理
            SettingsSection(
                title = "数据管理",
                icon = Icons.Default.Info
            ) {
                SettingsItem(
                    title = "导出数据",
                    subtitle = "将房源配置导出为 JSON 文件分享保存",
                    onClick = {
                        val json = viewModel.exportPropertiesToJson()
                        if (json != null) {
                            viewModel.shareBackupText(json)
                        }
                    }
                )

                SettingsItem(
                    title = "导入数据",
                    subtitle = "从备份 JSON 恢复房源配置（先复制 JSON 文本）",
                    onClick = {
                        try {
                            val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                            val clipData = clipboard?.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString()
                                if (!text.isNullOrBlank()) {
                                    val count = viewModel.importPropertiesFromJson(text)
                                    if (count >= 0) {
                                        // 显示成功消息
                                    }
                                }
                            }
                        } catch (_: Exception) { }
                    }
                )

                SettingsItem(
                    title = "备份说明",
                    subtitle = "Android 自动备份已开启，重装后数据自动恢复",
                    onClick = { }
                )
            }

            // 关于信息
            SettingsSection(
                title = "关于",
                icon = Icons.Default.Info
            ) {
                SettingsItem(
                    title = "应用版本",
                    subtitle = BuildConfig.VERSION_NAME,
                    onClick = { }
                )

                SettingsItem(
                    title = "监控历史",
                    subtitle = "查看所有监控记录",
                    onClick = { /* TODO: navigate to history */ }
                )
            }
        }

        // OTA 弹窗
        if (showUpdateDialog && availableUpdate != null) {
            UpdateDialog(
                update = availableUpdate!!,
                onDismiss = {
                    showUpdateDialog = false
                    viewModel.clearUpdateState()
                },
                onUpdate = {
                    showUpdateDialog = false
                    viewModel.startDownload()
                }
            )
        }

        if (showDownloadDialog) {
            DownloadProgressDialog(
                progress = downloadProgress,
                onCancel = {
                    showDownloadDialog = false
                    viewModel.cancelDownload()
                }
            )
        }

        if (showInstallDialog) {
            InstallPromptDialog(
                onInstall = {
                    showInstallDialog = false
                    viewModel.triggerInstall()
                },
                onDismiss = {
                    showInstallDialog = false
                }
            )
        }

        // 检查间隔选择对话框
        if (showIntervalDialog) {
            CheckIntervalDialog(
                currentInterval = uiState.userSettings?.checkInterval ?: 5,
                onDismiss = { showIntervalDialog = false },
                onConfirm = { interval ->
                    viewModel.updateCheckInterval(interval)
                    showIntervalDialog = false
                }
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun CheckIntervalDialog(
    currentInterval: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val intervals = listOf(5, 10, 15, 30, 60, 120)
    var selected by remember { mutableIntStateOf(currentInterval) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "检查间隔",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "选择房源监控的自动检查频率",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                intervals.forEach { interval ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = interval }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == interval,
                            onClick = { selected = interval }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (interval) {
                                5 -> "5 分钟（推荐）"
                                10 -> "10 分钟"
                                15 -> "15 分钟"
                                30 -> "30 分钟"
                                60 -> "1 小时"
                                120 -> "2 小时"
                                else -> "$interval 分钟"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onConfirm(selected) }) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
