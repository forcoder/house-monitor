package com.housemonitor.ui.property

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.housemonitor.data.model.ChangeSummary
import com.housemonitor.data.model.ChangeType
import com.housemonitor.data.model.MonitorRecord
import com.housemonitor.data.model.Property
import com.housemonitor.ui.theme.AppColors
import com.housemonitor.ui.theme.AppShapes
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyCard(
    property: Property,
    latestRecord: MonitorRecord? = null,
    isChecking: Boolean = false,
    onToggleActive: () -> Unit = {},
    onDelete: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val statusColor = when {
        isChecking -> AppColors.pending
        property.lastCheckedAt == 0L || latestRecord == null -> AppColors.pending
        else -> {
            try {
                val dates = com.google.gson.Gson().fromJson<List<String>>(
                    latestRecord.unavailableDates,
                    object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                ) ?: emptyList()
                when {
                    dates.isEmpty() -> AppColors.available
                    dates.size <= 3 -> AppColors.partial
                    else -> AppColors.unavailable
                }
            } catch (_: Exception) {
                AppColors.pending
            }
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.cardMedium,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // 左侧状态色条
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(IntrinsicSize.Max)
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)
            ) {
                // 标题行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = property.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val platformLabel = when (property.platform) {
                            "meituan" -> "美团民宿"
                            "tujia" -> "途家"
                            "xiaozhu" -> "小猪民宿"
                            "muniao" -> "木鸟民宿"
                            else -> "其他"
                        }
                        Text(
                            text = platformLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (property.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = property.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    StatusPill(isActive = property.isActive)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // URL 显示
                Text(
                    text = property.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 状态信息
                StatusSection(property.lastCheckedAt, latestRecord, isChecking)

                Spacer(modifier = Modifier.height(10.dp))

                // 分割线
                Divider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 0.5.dp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 底部操作行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (property.lastCheckedAt > 0)
                            "最近检查 ${formatTimestamp(property.lastCheckedAt)}"
                        else
                            "创建于 ${formatTimestamp(property.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        FilledTonalIconButton(
                            onClick = onRefresh,
                            enabled = property.isActive && !isChecking,
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                contentColor = if (property.isActive)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "刷新", modifier = Modifier.size(16.dp))
                            }
                        }

                        FilledTonalIconButton(
                            onClick = onToggleActive,
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (property.isActive)
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            )
                        ) {
                            Icon(
                                if (property.isActive) Icons.Default.NotificationsOff else Icons.Default.PlayArrow,
                                contentDescription = if (property.isActive) "暂停监控" else "开始监控",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        FilledTonalIconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = AppShapes.dialog,
            title = { Text("删除房源") },
            text = { Text("确定要删除房源 \"${property.name}\" 吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun StatusPill(isActive: Boolean) {
    Surface(
        shape = AppShapes.pill,
        color = if (isActive)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = if (isActive) "监控中" else "已暂停",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (isActive)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusSection(lastCheckedAt: Long, latestRecord: MonitorRecord?, isChecking: Boolean = false) {
    if (isChecking) {
        StatusTag(icon = Icons.Default.Sync, text = "检查中...", color = AppColors.pending)
        return
    }
    val record = latestRecord
    if (record == null || lastCheckedAt == 0L) {
        StatusTag(icon = Icons.Default.Schedule, text = "尚未检查", color = AppColors.pending)
        return
    }

    val unavailableCount = try {
        val unavailableDates = com.google.gson.Gson().fromJson<List<String>>(
            record.unavailableDates,
            object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
        ) ?: emptyList()
        unavailableDates.size
    } catch (e: Exception) {
        0
    }

    val changeSummary = ChangeSummary.fromJson(record.changeSummary)
    val hasChange = changeSummary.changeType != ChangeType.NO_CHANGE.name
    val changeType = try {
        ChangeType.valueOf(changeSummary.changeType)
    } catch (_: Exception) {
        ChangeType.NO_CHANGE
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val (icon, statusText, statusColor) = when {
                unavailableCount == 0 -> Triple(Icons.Default.CheckCircle, "全部可订", AppColors.available)
                unavailableCount <= 3 -> Triple(Icons.Default.Warning, "部分无房", AppColors.partial)
                else -> Triple(Icons.Default.Cancel, "全部无房", AppColors.unavailable)
            }
            StatusTag(icon = icon, text = statusText, color = statusColor)

            if (unavailableCount > 0) {
                Text(
                    text = "$unavailableCount 天无房",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (hasChange) {
            Spacer(modifier = Modifier.height(6.dp))
            when (changeType) {
                ChangeType.BECAME_UNAVAILABLE -> {
                    ChangeIndicator(text = "新增 ${changeSummary.newlyUnavailable.size} 天无房", color = AppColors.unavailable)
                }
                ChangeType.BECAME_AVAILABLE -> {
                    ChangeIndicator(text = "释放 ${changeSummary.newlyAvailable.size} 天有房", color = AppColors.available)
                }
                ChangeType.PARTIAL_CHANGE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ChangeIndicator(text = "新增 ${changeSummary.newlyUnavailable.size} 天无房", color = AppColors.unavailable)
                        ChangeIndicator(text = "释放 ${changeSummary.newlyAvailable.size} 天有房", color = AppColors.available)
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun StatusTag(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Surface(
        shape = AppShapes.pill,
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
private fun ChangeIndicator(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
