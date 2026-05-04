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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.housemonitor.data.model.ChangeSummary
import com.housemonitor.data.model.ChangeType
import com.housemonitor.data.model.MonitorRecord
import com.housemonitor.data.model.Property
import com.housemonitor.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyCard(
    property: Property,
    latestRecord: MonitorRecord? = null,
    onToggleActive: () -> Unit = {},
    onDelete: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 计算状态色
    val statusColor = when {
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
            } catch (_: Exception) { AppColors.pending }
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                    .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
            ) {
                // 标题行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (property.description.isNotEmpty()) {
                            Text(
                                text = property.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 状态指示器
                    StatusIndicator(isActive = property.isActive)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // URL显示
                Text(
                    text = property.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 状态信息区域
                StatusSection(property.lastCheckedAt, latestRecord)

                Spacer(modifier = Modifier.height(8.dp))

                // 底部信息行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 时间信息
                    Text(
                        text = if (property.lastCheckedAt > 0)
                            "最近检查 ${formatTimestamp(property.lastCheckedAt)}"
                        else
                            "创建于 ${formatTimestamp(property.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 操作按钮
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onRefresh,
                            enabled = property.isActive,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "刷新",
                                modifier = Modifier.size(18.dp),
                                tint = if (property.isActive)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }

                        IconButton(
                            onClick = onToggleActive,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (property.isActive) Icons.Default.NotificationsOff else Icons.Default.PlayArrow,
                                contentDescription = if (property.isActive) "暂停监控" else "开始监控",
                                modifier = Modifier.size(18.dp),
                                tint = if (property.isActive)
                                    MaterialTheme.colorScheme.secondary
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除房源") },
            text = { Text("确定要删除房源 \"${property.name}\" 吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除")
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
fun StatusIndicator(isActive: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isActive)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = if (isActive) "监控中" else "已暂停",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
private fun StatusSection(lastCheckedAt: Long, latestRecord: MonitorRecord?) {
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
    val changeType = try { ChangeType.valueOf(changeSummary.changeType) } catch (_: Exception) { ChangeType.NO_CHANGE }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 当前状态行
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

        // 状态变化行
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
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
            color = color
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
