package com.housemonitor.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.housemonitor.data.model.ChangeSummary
import com.housemonitor.data.model.ChangeType
import com.housemonitor.data.model.MonitorRecord
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MonitorRecordCard(
    record: MonitorRecord,
    isFirst: Boolean = false,
    propertyName: String = ""
) {
    var expanded by remember { mutableStateOf(false) }
    val changeSummary = ChangeSummary.fromJson(record.changeSummary)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 时间轴圆点
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(getChangeTypeColor(changeSummary.changeType))
                )
                if (!isFirst) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(40.dp)
                            .background(Color.Gray.copy(alpha = 0.3f))
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 卡片内容
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // 头部：日期 + 变化类型标签
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = record.checkDate,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        ChangeTypeChip(changeSummary.changeType)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 摘要文本
                    Text(
                        text = getChangeSummaryText(changeSummary, propertyName),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 时间和展开图标
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimestamp(record.checkedAt),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "收起" else "展开",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 展开详情
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Divider(color = Color.Gray.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))

                            if (changeSummary.newlyUnavailable.isNotEmpty()) {
                                Text(
                                    text = "🔴 新增无房日期 (${changeSummary.newlyUnavailable.size}天)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE53935)
                                )
                                changeSummary.newlyUnavailable.sorted().forEach { date ->
                                    Text(
                                        text = "  • $date",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            if (changeSummary.newlyAvailable.isNotEmpty()) {
                                Text(
                                    text = "🟢 释放有房日期 (${changeSummary.newlyAvailable.size}天)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF43A047)
                                )
                                changeSummary.newlyAvailable.sorted().forEach { date ->
                                    Text(
                                        text = "  • $date",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            if (changeSummary.changeType == ChangeType.NO_CHANGE.name) {
                                Text(
                                    text = "✅ 无变化",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 显示不可用日期总数
                            Spacer(modifier = Modifier.height(6.dp))
                            val dates = try {
                                com.google.gson.Gson().fromJson(record.unavailableDates, Array<String>::class.java) ?: emptyArray()
                            } catch (e: Exception) {
                                emptyArray()
                            }
                            Text(
                                text = "当前无房日期：${dates.size}天",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
        Divider(color = Color.Gray.copy(alpha = 0.15f))
    }
}

@Composable
fun ChangeTypeChip(changeType: String) {
    val (text, color) = when (changeType) {
        ChangeType.BECAME_UNAVAILABLE.name -> "新增无房" to Color(0xFFE53935)
        ChangeType.BECAME_AVAILABLE.name -> "释放有房" to Color(0xFF43A047)
        ChangeType.PARTIAL_CHANGE.name -> "部分变化" to Color(0xFFFB8C00)
        else -> "无变化" to Color(0xFF757575)
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.padding(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

private fun getChangeTypeColor(changeType: String): Color {
    return when (changeType) {
        ChangeType.BECAME_UNAVAILABLE.name -> Color(0xFFE53935)
        ChangeType.BECAME_AVAILABLE.name -> Color(0xFF43A047)
        ChangeType.PARTIAL_CHANGE.name -> Color(0xFFFB8C00)
        else -> Color(0xFF757575)
    }
}

private fun getChangeSummaryText(summary: ChangeSummary, propertyName: String): String {
    return when (summary.changeType) {
        ChangeType.BECAME_UNAVAILABLE.name ->
            "${propertyName}：${summary.newlyUnavailable.size}个日期变为无房"
        ChangeType.BECAME_AVAILABLE.name ->
            "${propertyName}：${summary.newlyAvailable.size}个日期变为有房"
        ChangeType.PARTIAL_CHANGE.name ->
            "${propertyName}：${summary.newlyUnavailable.size}个日期变为无房，${summary.newlyAvailable.size}个日期变为有房"
        else -> "${propertyName}：房源状态无变化"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
