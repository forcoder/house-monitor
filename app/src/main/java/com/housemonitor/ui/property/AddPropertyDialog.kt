package com.housemonitor.ui.property

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.housemonitor.service.PlatformParserFactory
import com.housemonitor.ui.theme.AppShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPropertyDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf("meituan") }
    var platformDropdownExpanded by remember { mutableStateOf(false) }
    var showUrlExample by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val platforms = listOf(
        "meituan" to "美团民宿",
        "tujia" to "途家",
        "xiaozhu" to "小猪民宿",
        "muniao" to "木鸟民宿"
    )

    val nameFocusRequester = remember { FocusRequester() }
    val platformParserFactory = remember { PlatformParserFactory() }

    LaunchedEffect(Unit) {
        nameFocusRequester.requestFocus()
    }

    LaunchedEffect(url) {
        if (url.isNotBlank()) {
            val detected = platformParserFactory.detectPlatform(url)
            selectedPlatform = detected
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = AppShapes.dialog,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 标题
                Text(
                    text = "添加房源",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // 房源名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("房源名称") },
                    placeholder = { Text("例如：北京朝阳区温馨公寓") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocusRequester),
                    singleLine = true,
                    shape = AppShapes.button
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 平台选择
                ExposedDropdownMenuBox(
                    expanded = platformDropdownExpanded,
                    onExpandedChange = { platformDropdownExpanded = !platformDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = platforms.find { it.first == selectedPlatform }?.second ?: "美团民宿",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("平台") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = platformDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = AppShapes.button
                    )
                    ExposedDropdownMenu(
                        expanded = platformDropdownExpanded,
                        onDismissRequest = { platformDropdownExpanded = false }
                    ) {
                        platforms.forEach { (id, displayName) ->
                            DropdownMenuItem(
                                text = { Text(displayName) },
                                onClick = {
                                    selectedPlatform = id
                                    platformDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 链接输入
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("小程序链接") },
                    placeholder = { Text("https://...meituan.com/...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    trailingIcon = {
                        IconButton(onClick = { showUrlExample = !showUrlExample }) {
                            Icon(
                                if (showUrlExample) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (showUrlExample) "隐藏示例" else "显示示例"
                            )
                        }
                    },
                    shape = AppShapes.button
                )

                if (showUrlExample) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.cardSmall,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "链接格式示例：",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "美团民宿小程序：\n小程序内长按识别二维码\n或复制小程序链接",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 描述（可选）
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("备注（可选）") },
                    placeholder = { Text("备注信息...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    shape = AppShapes.button
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && url.isNotBlank()) {
                                isLoading = true
                                onConfirm(name.trim(), url.trim(), description.trim(), selectedPlatform)
                            }
                        },
                        enabled = name.isNotBlank() && url.isNotBlank() && !isLoading,
                        shape = AppShapes.button
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("添加")
                    }
                }
            }
        }
    }
}
