package com.housemonitor.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.housemonitor.R
import com.housemonitor.data.model.MonitorRecord
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    preselectedProperty: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }

    // 如果有预选择的房源，自动过滤
    LaunchedEffect(preselectedProperty) {
        preselectedProperty?.let {
            viewModel.filterByProperty(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("监控历史") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.List, contentDescription = "筛选")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 筛选标签栏
            if (uiState.selectedPropertyId != null) {
                FilterChipRow(
                    propertyName = uiState.propertyName ?: "未知房源",
                    onClearFilter = { viewModel.clearFilter() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 记录列表
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.filteredRecords.isEmpty() -> {
                    EmptyHistoryState(
                        modifier = Modifier.fillMaxSize(),
                        hasFilter = uiState.selectedPropertyId != null,
                        onClearFilter = { viewModel.clearFilter() }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(uiState.filteredRecords) { index, record ->
                            MonitorRecordCard(
                                record = record,
                                isFirst = index == 0,
                                propertyName = uiState.allProperties.find { it.id == record.propertyId }?.name ?: "未知房源"
                            )
                        }
                    }
                }
            }
        }
    }

    // 筛选对话框
    if (showFilterDialog) {
        FilterDialog(
            properties = uiState.allProperties,
            selectedPropertyId = uiState.selectedPropertyId,
            onPropertySelected = { propertyId, name ->
                viewModel.filterByProperty(propertyId)
                showFilterDialog = false
            },
            onClearFilter = {
                viewModel.clearFilter()
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}

@Composable
fun FilterChipRow(
    propertyName: String,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "显示：$propertyName 的历史记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            TextButton(onClick = onClearFilter) {
                Text("清除筛选")
            }
        }
    }
}

@Composable
fun EmptyHistoryState(
    modifier: Modifier = Modifier,
    hasFilter: Boolean = false,
    onClearFilter: () -> Unit = {}
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (hasFilter) "暂无该房源的监控记录" else "暂无监控记录",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (hasFilter) "该房源可能还没有被监控过，或所有记录已被清理" else "开始添加房源并启用监控后，这里将显示历史记录",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (hasFilter) {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onClearFilter) {
                Text("查看所有记录")
            }
        }
    }
}

@Composable
fun FilterDialog(
    properties: List<com.housemonitor.data.model.Property>,
    selectedPropertyId: String?,
    onPropertySelected: (String, String) -> Unit,
    onClearFilter: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("筛选房源") },
        text = {
            Column {
                // 显示所有记录选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedPropertyId == null,
                        onClick = { onClearFilter() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("显示所有房源的记录")
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // 按房源筛选选项
                Text(
                    text = "按房源筛选：",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                properties.forEach { property ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPropertyId == property.id,
                            onClick = { onPropertySelected(property.id, property.name) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = property.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (property.description.isNotEmpty()) {
                                Text(
                                    text = property.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}