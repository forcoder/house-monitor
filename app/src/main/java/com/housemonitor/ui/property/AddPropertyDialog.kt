package com.housemonitor.ui.property

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

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

    val platformOptions = listOf(
        "meituan" to "美团民宿",
        "other" to "其他平台"
    )

    val nameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        nameFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "添加房源",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 房源名称输入
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("房源名称") },
                    placeholder = { Text("例如：北京朝阳区温馨公寓") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocusRequester),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 平台选择
                ExposedDropdownMenuBox(
                    expanded = platformDropdownExpanded,
                    onExpandedChange = { platformDropdownExpanded = !platformDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = platformOptions.find { it.first == selectedPlatform }?.second ?: "美团民宿",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("平台") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = platformDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = platformDropdownExpanded,
                        onDismissRequest = { platformDropdownExpanded = false }
                    ) {
                        platformOptions.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedPlatform = id
                                    platformDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 美团小程序链接输入
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("美团小程序链接") },
                    placeholder = { Text("https://...meituan.com/...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    trailingIcon = {
                        IconButton(onClick = { showUrlExample = !showUrlExample }) {
                            Icon(
                                if (showUrlExample) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showUrlExample) "隐藏示例" else "显示示例"
                            )
                        }
                    }
                )

                // URL示例
                if (showUrlExample) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "链接格式示例：",
                                style = MaterialTheme.typography.labelMedium,
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

                // 描述输入（可选）
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    placeholder = { Text("备注信息...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
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
                        enabled = name.isNotBlank() && url.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
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