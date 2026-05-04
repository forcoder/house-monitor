package com.housemonitor.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.housemonitor.data.model.MonitorRecord
import com.housemonitor.ui.property.AddPropertyDialog
import com.housemonitor.ui.property.PropertyCard
import com.housemonitor.ui.theme.AppColors
import com.housemonitor.ui.theme.AppGradients
import com.housemonitor.ui.theme.AppShapes
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
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "房源监控",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.properties.isNotEmpty()) {
                            Text(
                                text = "${uiState.properties.count { it.isActive }} 个房源监控中",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("添加房源") },
                shape = AppShapes.button,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
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
    var noRecordCount = 0

    properties.filter { it.isActive }.forEach { property ->
        val record = latestRecords[property.id]
        if (record == null || property.lastCheckedAt == 0L) {
            noRecordCount++
        } else {
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
            } catch (_: Exception) {
                noRecordCount++
            }
        }
    }

    val checkedTotal = availableCount + partialCount + unavailableCount

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.cardLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppGradients.dashboard)
                .padding(20.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "状态概览",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                FilledTonalButton(
                    onClick = onCheckAll,
                    enabled = !isRefreshing && active > 0,
                    shape = AppShapes.pill,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
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
                    Text("全部检查", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 超大数字仪表盘
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DashboardStatItem(
                    value = "$availableCount",
                    label = "可订",
                    subLabel = "套房源",
                    color = Color(0xFF69F0AE)
                )
                DashboardStatItem(
                    value = "$unavailableCount",
                    label = "无房",
                    subLabel = "套房源",
                    color = Color(0xFFFF8A80)
                )
                DashboardStatItem(
                    value = "$total",
                    label = "总计",
                    subLabel = "套房源",
                    color = Color.White
                )
            }

            // 状态分布条（仅在有检查数据时显示）
            if (checkedTotal > 0) {
                Spacer(modifier = Modifier.height(20.dp))

                val availableRatio = availableCount.toFloat() / checkedTotal
                val partialRatio = partialCount.toFloat() / checkedTotal
                val unavailableRatio = unavailableCount.toFloat() / checkedTotal

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    if (availableRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(availableRatio)
                                .fillMaxHeight()
                                .background(Color(0xFF69F0AE))
                        )
                    }
                    if (partialRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(partialRatio)
                                .fillMaxHeight()
                                .background(Color(0xFFFFD54F))
                        )
                    }
                    if (unavailableRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(unavailableRatio)
                                .fillMaxHeight()
                                .background(Color(0xFFFF8A80))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendDot(color = Color(0xFF69F0AE), label = "可订", count = availableCount)
                    LegendDot(color = Color(0xFFFFD54F), label = "部分", count = partialCount)
                    LegendDot(color = Color(0xFFFF8A80), label = "无房", count = unavailableCount)
                }
            }

            // 次要统计行
            if (noRecordCount > 0 || partialCount > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SmallChip(value = "$partialCount", label = "部分无房", color = Color(0xFFFFD54F))
                    SmallChip(value = "$noRecordCount", label = "待检查", color = Color.White.copy(alpha = 0.7f))
                    SmallChip(value = "$active", label = "监控中", color = Color(0xFF82B1FF))
                }
            }

            if (unchecked > 0 && checked == 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$unchecked 个房源尚未检查，点击「全部检查」开始",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardStatItem(
    value: String,
    label: String,
    subLabel: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            lineHeight = 40.sp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = subLabel,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun SmallChip(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String, count: Int) {
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
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier, onAddProperty: () -> Unit) {
    Column(
        modifier = modifier.padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(60.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "还没有添加房源",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "添加你要监控的房源链接\n实时追踪可订状态变化",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onAddProperty,
            shape = AppShapes.button,
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加第一个房源", style = MaterialTheme.typography.labelLarge)
        }
    }
}
