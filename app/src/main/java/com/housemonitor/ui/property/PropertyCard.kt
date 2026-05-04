package com.housemonitor.ui.property

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = property.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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

            Spacer(modifier = Modifier.height(12.dp))

            // URL显示
            Text(
                text = property.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 状态信息区域
            StatusSection(property.lastCheckedAt, latestRecord)

            Spacer(modifier = Modifier.height(12.dp))

            // 底部信息行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 时间信息
                Column {
                    Text(
                        text = if (property.lastCheckedAt > 0) "最近检查" else "创建时间",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (property.lastCheckedAt > 0)
                            formatTimestamp(property.lastCheckedAt)
                        else
                            formatTimestamp(property.createdAt),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // 操作按钮
                Row {
                    IconButton(
                        onClick = onRefresh,
                        enabled = property.isActive
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = if (property.isActive)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onToggleActive
                    ) {
                        Icon(
                            if (property.isActive) Icons.Default.Notifications else Icons.Default.PlayArrow,
                            contentDescription = if (property.isActive) "暂停监控" else "开始监控",
                            tint = if (property.isActive)
                                MaterialTheme.colorScheme.secondary
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
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
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun StatusIndicator(isActive: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = if (isActive) "监控中" else "已暂停",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
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
        // 从未检查过
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "尚未检查",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 当前状态行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 状态标签
            val (statusText, statusColor) = when {
                unavailableCount == 0 -> "全部可订" to Color(0xFF4CAF50)
                unavailableCount <= 3 -> "部分无房" to Color(0xFFFF9800)
                else -> "全部无房" to Color(0xFFF44336)
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = statusColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }

            // 无房日期数
            if (unavailableCount > 0) {
                Text(
                    text = "$unavailableCount 天无房",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 状态变化行（仅在有变化时显示）
        if (hasChange) {
            Spacer(modifier = Modifier.height(6.dp))
            when (changeType) {
                ChangeType.BECAME_UNAVAILABLE -> {
                    ChangeIndicator(
                        text = "新增 ${changeSummary.newlyUnavailable.size} 天无房",
                        color = Color(0xFFF44336)
                    )
                }
                ChangeType.BECAME_AVAILABLE -> {
                    ChangeIndicator(
                        text = "释放 ${changeSummary.newlyAvailable.size} 天有房",
                        color = Color(0xFF4CAF50)
                    )
                }
                ChangeType.PARTIAL_CHANGE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChangeIndicator(
                            text = "新增 ${changeSummary.newlyUnavailable.size} 天无房",
                            color = Color(0xFFF44336)
                        )
                        ChangeIndicator(
                            text = "释放 ${changeSummary.newlyAvailable.size} 天有房",
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ChangeIndicator(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
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