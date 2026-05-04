package com.housemonitor.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.housemonitor.data.model.MonitorRecord
import com.housemonitor.ui.property.AddPropertyDialog
import com.housemonitor.ui.property.PropertyCard
import com.housemonitor.ui.theme.AppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }

    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            scope.launch { viewModel.clearError() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "房源监控",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.properties.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text("${uiState.properties.size}")
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshAllProperties() },
                        enabled = !uiState.isRefreshing
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                    IconButton(
                        onClick = { viewModel.refreshAllProperties() },
                        enabled = !uiState.isRefreshing
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "全部检查")
                    }
                    TextButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("设置")
                    }
                    IconButton(onClick = { /* TODO: navigate to history */ }) {
                        Icon(Icons.Default.List, contentDescription = "历史记录")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("添加房源") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.properties.isEmpty() -> {
                    EmptyState(
                        modifier = Modifier.align(Alignment.Center),
                        onAddProperty = { showAddDialog = true }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            StatusSummaryCard(
                                properties = uiState.properties,
                                latestRecords = uiState.latestRecords,
                                isRefreshing = uiState.isRefreshing,
                                onCheckAll = { viewModel.refreshAllProperties() }
                            )
                        }
                        items(uiState.properties) { property ->
                            PropertyCard(
                                property = property,
                                latestRecord = uiState.latestRecords[property.id],
                                onToggleActive = { viewModel.togglePropertyActive(property.id) },
                                onDelete = { viewModel.removeProperty(property.id) },
                                onRefresh = { viewModel.refreshSingleProperty(property.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPropertyDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, url, description, platform ->
                viewModel.addProperty(name, url, description, platform)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun StatusSummaryCard(
    properties: List<com.housemonitor.data.model.Property>,
    latestRecords: Map<String, MonitorRecord?>,
    isRefreshing: Boolean,
    onCheckAll: () -> Unit
) {
    val total = properties.size
    val active = properties.count { it.isActive }
    val checked = properties.count { it.lastCheckedAt > 0 }
    val unchecked = total - checked

    var availableCount = 0
    var partialCount = 0
    var unavailableCount = 0

    properties.filter { it.isActive && it.lastCheckedAt > 0 }.forEach { property ->
        val record = latestRecords[property.id] ?: return@forEach
        try {
            val dates = com.google.gson.Gson().fromJson<List<String>>(
                record.unavailableDates,
                object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
            ) ?: emptyList()
            when {
                dates.isEmpty() -> availableCount++
                dates.size <= 3 -> partialCount++
                else -> unavailableCount++
            }
        } catch (_: Exception) { /* skip */ }
    }

    val checkedTotal = availableCount + partialCount + unavailableCount

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // 标题行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "状态概览",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalButton(
                        onClick = onCheckAll,
                        enabled = !isRefreshing && active > 0,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text("全部检查", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 大数字统计行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BigStatItem(label = "总房源", value = total, icon = Icons.Default.Home, color = MaterialTheme.colorScheme.onSurface)
                    BigStatItem(label = "监控中", value = active, icon = Icons.Default.Visibility, color = MaterialTheme.colorScheme.primary)
                    BigStatItem(label = "已检查", value = checked, icon = Icons.Default.CheckCircle, color = AppColors.available)
                }

                // 状态分布条（仅在有检查数据时显示）
                if (checkedTotal > 0) {
                    Spacer(modifier = Modifier.height(16.dp))

                    val availableRatio = availableCount.toFloat() / checkedTotal
                    val partialRatio = partialCount.toFloat() / checkedTotal
                    val unavailableRatio = unavailableCount.toFloat() / checkedTotal

                    // 分布条形图
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        if (availableRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(availableRatio)
                                    .fillMaxHeight()
                                    .background(AppColors.available)
                            )
                        }
                        if (partialRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(partialRatio)
                                    .fillMaxHeight()
                                    .background(AppColors.partial)
                            )
                        }
                        if (unavailableRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(unavailableRatio)
                                    .fillMaxHeight()
                                    .background(AppColors.unavailable)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 图例
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendItem(color = AppColors.available, label = "可订", count = availableCount)
                        LegendItem(color = AppColors.partial, label = "部分", count = partialCount)
                        LegendItem(color = AppColors.unavailable, label = "无房", count = unavailableCount)
                    }
                }

                if (unchecked > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$unchecked 个房源尚未检查",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BigStatItem(label: String, value: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$value",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label $count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier, onAddProperty: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Home,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "还没有添加房源",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击下方按钮添加要监控的房源",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddProperty,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加房源")
        }
    }
}
