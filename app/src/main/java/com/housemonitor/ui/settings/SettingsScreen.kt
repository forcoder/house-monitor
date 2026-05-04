package com.housemonitor.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

    // OTA 状态
    val updateStatus by viewModel.updateStatus.collectAsState()
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    // Dialog 显示控制
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showInstallDialog by remember { mutableStateOf(false) }

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
            // 监控设置
            SettingsSection(
                title = "监控设置",
                icon = Icons.Default.Info
            ) {
                SettingsItem(
                    title = "检查间隔",
                    subtitle = "${uiState.userSettings?.checkInterval ?: 60} 分钟",
                    onClick = {
                        // 显示间隔选择对话框
                    }
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
                    title = stringResource(R.string.check_for_update),
                    subtitle = when (updateStatus) {
                        UpdateStatus.CHECKING -> stringResource(R.string.checking_update)
                        UpdateStatus.DOWNLOADING -> "${stringResource(R.string.downloading_update)} ${(downloadProgress * 100).toInt()}%"
                        UpdateStatus.UPDATE_AVAILABLE -> "${stringResource(R.string.update_available)}：${availableUpdate?.versionName ?: ""}"
                        UpdateStatus.DOWNLOADED -> stringResource(R.string.download_complete)
                        else -> "当前版本 ${BuildConfig.VERSION_NAME}"
                    },
                    onClick = { viewModel.checkForUpdate() },
                    trailing = if (updateStatus == UpdateStatus.CHECKING) {
                        { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
                    } else null
                )

                SettingsItem(
                    title = "隐私政策",
                    subtitle = "查看隐私政策",
                    onClick = { }
                )

                SettingsItem(
                    title = "用户协议",
                    subtitle = "查看用户协议",
                    onClick = { }
                )

                Spacer(modifier = Modifier.height(8.dp))

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
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
