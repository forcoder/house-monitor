package com.housemonitor.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.housemonitor.R
import com.housemonitor.data.model.OtaUpdate

/**
 * 发现新版本弹窗
 */
@Composable
fun UpdateDialog(
    update: OtaUpdate,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    val isForceUpdate = update.isForceUpdate

    AlertDialog(
        onDismissRequest = { if (!isForceUpdate) onDismiss() },
        title = {
            Text(text = stringResource(R.string.update_dialog_title, update.versionName))
        },
        text = {
            Column {
                if (update.releaseNotes.isNotBlank()) {
                    Text(
                        text = update.releaseNotes,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onUpdate) {
                Text(stringResource(R.string.update_now))
            }
        },
        dismissButton = if (!isForceUpdate) {
            {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.update_later))
                }
            }
        } else null
    )
}

/**
 * 下载进度弹窗
 */
@Composable
fun DownloadProgressDialog(
    progress: Float,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* 下载中不可关闭 */ },
        title = { Text(stringResource(R.string.downloading_update)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel_download))
            }
        }
    )
}

/**
 * 安装提示弹窗
 */
@Composable
fun InstallPromptDialog(
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.download_complete)) },
        text = { Text("新版本已下载完成，是否立即安装？") },
        confirmButton = {
            Button(onClick = onInstall) {
                Text(stringResource(R.string.install_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.update_later))
            }
        }
    )
}
