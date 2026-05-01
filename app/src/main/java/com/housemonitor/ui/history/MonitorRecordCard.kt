package com.housemonitor.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.housemonitor.data.model.MonitorRecord
import com.housemonitor.util.NotificationManager
import com.housemonitor.data.repository.MonitorRepository
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorRecordCard(
    record: MonitorRecord,
    modifier: Modifier = Modifier
) {
    val monitorRepository = runBlocking { MonitorRepository(null, com.google.gson.Gson()) } // 临时获取依赖

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (record.status) {
                "success" -> MaterialTheme.colorScheme.surface
                "failed" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题行：日期和状态指示器
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.checkDate,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                StatusIndicator(
                    status = record.status,
                    isAvailable = isPropertyAvailable(record, monitorRepository)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 房源信息
            val propertyId = record.propertyId // 实际项目中应该通过Repository获取
            Text(
                text = "房源ID: $propertyId", // 临时显示，实际应该显示房源名称
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 无房日期列表
            if (record.unavailableDates.isNotEmpty()) {
                val unavailableDates = runBlocking {
                    try {
                        monitorRepository.parseUnavailableDates(record.unavailableDates)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                if (unavailableDates.isNotEmpty()) {
                    Text(
                        text = "无房日期：",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    unavailableDates.forEach { date ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = date,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            } else {
                // 有房状态
                Surface(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "有房",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 底部信息行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 检查时间
                Text(
                    text = formatTimestamp(record.checkedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 操作按钮区域（可以添加查看详情等功能）
                Row {
                    // 状态图标
                    Icon(
                        imageVector = getStatusIcon(record.status),
                        contentDescription = "状态",
                        tint = getStatusColor(record.status),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(
    status: String,
    isAvailable: Boolean
) {
    val (backgroundColor, textColor, text) = when {
        status == "failed" -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "检查失败"
        )
        !isAvailable -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "无房"
        )
        else -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "有房"
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (!isAvailable && status != "failed") Icons.Default.Error else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
    }
}

private fun getStatusIcon(status: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (status) {
        "success" -> Icons.Default.CheckCircle
        "failed" -> Icons.Default.Error
        else -> Icons.Default.Warning
    }
}

private fun getStatusColor(status: String): Color {
    return when (status) {
        "success" -> MaterialTheme.colorScheme.primary
        "failed" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun isPropertyAvailable(record: MonitorRecord, repository: MonitorRepository): Boolean {
    return try {
        val dates = repository.parseUnavailableDates(record.unavailableDates)
        dates.isEmpty() // 如果没有无房日期，则认为有房
    } catch (e: Exception) {
        false // 解析失败时保守处理为无房
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(java.util.Date(timestamp))
}