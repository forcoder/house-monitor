# 多平台支持 + 审计历史 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增途家/小猪民宿/木鸟民宿三平台支持，实现每房源 10 条审计历史 + 变化追踪 + 时间轴 UI

**Architecture:** PlatformParser 接口扩展 → 3 个新 Parser → Room DB 迁移 → 变化检测逻辑 → UI 更新

**Tech Stack:** Kotlin, Room, Hilt, Jetpack Compose, WebView JS Injection

---

### Task A: 数据库迁移 — MonitorRecord 添加 changeSummary 字段

**Files:**
- Modify: `app/src/main/java/com/housemonitor/data/model/MonitorRecord.kt`
- Modify: `app/src/main/java/com/housemonitor/data/database/HouseMonitorDatabase.kt`
- Modify: `app/src/main/java/com/housemonitor/data/database/MonitorRecordDao.kt`
- Modify: `app/src/main/java/com/housemonitor/data/repository/MonitorRepository.kt`

- [ ] **Step 1: 修改 MonitorRecord 数据模型，添加 changeSummary 字段**

```kotlin
// app/src/main/java/com/housemonitor/data/model/MonitorRecord.kt
@Entity(
    tableName = "monitor_records",
    foreignKeys = [ForeignKey(
        entity = Property::class,
        parentColumns = ["id"],
        childColumns = ["propertyId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class MonitorRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val propertyId: String,
    val checkDate: String,           // yyyy-MM-dd格式
    val unavailableDates: String,    // JSON格式存储日期列表
    val checkedAt: Long = System.currentTimeMillis(),
    val status: String = "success",  // success, failed, timeout
    val changeSummary: String = ""   // JSON: {newlyUnavailable, newlyAvailable, changeType}
)
```

- [ ] **Step 2: 添加数据库迁移 MIGRATION_2_3，升级版本到 3**

```kotlin
// HouseMonitorDatabase.kt 中修改:
// version = 2 → version = 3 (注意: 当前代码已经是 version=2, 但 build.gradle versionCode=3,
// 这里数据库版本独立于 app versionCode)

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE monitor_records ADD COLUMN changeSummary TEXT NOT NULL DEFAULT ''"
        )
    }
}

// getDatabase() 中 .addMigrations(MIGRATION_1_2) → .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
```

- [ ] **Step 3: 在 MonitorRecordDao 中新增查询方法**

```kotlin
// 新增 import: List<MonitorRecord> (非 Flow)

// 获取某房源最近 N 条记录（用于历史页面）
@Query("SELECT * FROM monitor_records WHERE propertyId = :propertyId ORDER BY checkedAt DESC LIMIT :limit")
suspend fun getRecentRecordsByPropertyId(propertyId: String, limit: Int = 10): List<MonitorRecord>

// 清理旧记录（每房源只保留最近 10 条）
@Query("DELETE FROM monitor_records WHERE propertyId = :propertyId AND id NOT IN (SELECT id FROM monitor_records WHERE propertyId = :propertyId ORDER BY checkedAt DESC LIMIT 10)")
suspend fun cleanupOldRecordsByPropertyId(propertyId: String)
```

- [ ] **Step 4: 在 MonitorRepository 中新增方法**

```kotlin
suspend fun getRecentRecordsByPropertyId(propertyId: String, limit: Int = 10): List<MonitorRecord> {
    return monitorRecordDao.getRecentRecordsByPropertyId(propertyId, limit)
}

suspend fun cleanupOldRecordsByPropertyId(propertyId: String) {
    monitorRecordDao.cleanupOldRecordsByPropertyId(propertyId)
}
```

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "feat: 数据库迁移 - MonitorRecord 添加 changeSummary 字段 + 每房源10条记录清理"
```

---

### Task B: 创建 ChangeSummary 数据模型 + ChangeType 枚举

**Files:**
- Create: `app/src/main/java/com/housemonitor/data/model/ChangeSummary.kt`

- [ ] **Step 1: 创建 ChangeSummary.kt**

```kotlin
package com.housemonitor.data.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * 记录两次检查之间的变化摘要
 */
data class ChangeSummary(
    @SerializedName("newlyUnavailable") val newlyUnavailable: List<String> = emptyList(),
    @SerializedName("newlyAvailable") val newlyAvailable: List<String> = emptyList(),
    @SerializedName("changeType") val changeType: String = "NO_CHANGE"
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): ChangeSummary {
            return try {
                Gson().fromJson(json, ChangeSummary::class.java)
            } catch (e: Exception) {
                ChangeSummary()
            }
        }

        fun noChange() = ChangeSummary(changeType = "NO_CHANGE")
    }
}

enum class ChangeType {
    NO_CHANGE,           // 无变化
    BECAME_UNAVAILABLE,  // 新增无房日期
    BECAME_AVAILABLE,    // 释放有房日期
    PARTIAL_CHANGE       // 部分日期变化（同时有新增和释放）
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/housemonitor/data/model/ChangeSummary.kt
git commit -m "feat: 添加 ChangeSummary 数据模型"
```

---

### Task C: 创建三个新平台 Parser

**Files:**
- Create: `app/src/main/java/com/housemonitor/service/TujiaPlatformParser.kt`
- Create: `app/src/main/java/com/housemonitor/service/XiaozhuPlatformParser.kt`
- Create: `app/src/main/java/com/housemonitor/service/MuniaoPlatformParser.kt`
- Modify: `app/src/main/java/com/housemonitor/service/PlatformParserFactory.kt`

- [ ] **Step 1: 创建 TujiaPlatformParser.kt**

途家日历特点：使用 `.calendar-panel`、`.day-item` 等选择器，不可用状态通过 `.disabled`、`.off`、`.sold-out` 标记。

```kotlin
package com.housemonitor.service

class TujiaPlatformParser : PlatformParser {
    override val platformId: String = "tujia"
    override val platformName: String = "途家"

    override fun matchesUrl(url: String): Boolean {
        return url.contains("tujia.com") || url.contains("tujia")
    }

    override fun buildCalendarDetectionJs(): String {
        return """
            (function() {
                var results = [];
                // 途家日历选择器
                var cells = document.querySelectorAll('.calendar-panel .day-item, .day-cell, .date-item, [data-date]');
                var unavailableClasses = ['disabled', 'off', 'sold-out', 'unavailable', 'booked', 'full', 'closed'];
                
                for (var i = 0; i < cells.length; i++) {
                    var cell = cells[i];
                    var text = cell.textContent.trim();
                    var className = cell.className || '';
                    var isUnavailable = false;
                    
                    // 检查 class
                    for (var j = 0; j < unavailableClasses.length; j++) {
                        if (className.indexOf(unavailableClasses[j]) !== -1) {
                            isUnavailable = true;
                            break;
                        }
                    }
                    
                    // 检查 data 属性
                    if (!isUnavailable && cell.getAttribute('data-available') === 'false') {
                        isUnavailable = true;
                    }
                    if (!isUnavailable && cell.getAttribute('data-status') === 'unavailable') {
                        isUnavailable = true;
                    }
                    
                    if (isUnavailable) {
                        var dateStr = cell.getAttribute('data-date') || cell.getAttribute('data-day') || '';
                        if (dateStr) {
                            results.push(dateStr);
                        }
                    }
                }
                
                return JSON.stringify(results);
            })();
        """.trimIndent()
    }
}
```

- [ ] **Step 2: 创建 XiaozhuPlatformParser.kt**

小猪民宿日历特点：使用 `.room-calendar`、`.day` 等选择器，不可用状态通过 `.disabled`、`.not-available` 标记。

```kotlin
package com.housemonitor.service

class XiaozhuPlatformParser : PlatformParser {
    override val platformId: String = "xiaozhu"
    override val platformName: String = "小猪民宿"

    override fun matchesUrl(url: String): Boolean {
        return url.contains("xiaozhu.com") || url.contains("xiaozhu")
    }

    override fun buildCalendarDetectionJs(): String {
        return """
            (function() {
                var results = [];
                var cells = document.querySelectorAll('.room-calendar .day, .calendar-day, .date-cell, [data-date]');
                var unavailableClasses = ['disabled', 'not-available', 'unavailable', 'booked', 'occupied', 'full'];
                
                for (var i = 0; i < cells.length; i++) {
                    var cell = cells[i];
                    var className = cell.className || '';
                    var isUnavailable = false;
                    
                    for (var j = 0; j < unavailableClasses.length; j++) {
                        if (className.indexOf(unavailableClasses[j]) !== -1) {
                            isUnavailable = true;
                            break;
                        }
                    }
                    
                    if (!isUnavailable && cell.getAttribute('data-available') === 'false') {
                        isUnavailable = true;
                    }
                    
                    if (isUnavailable) {
                        var dateStr = cell.getAttribute('data-date') || cell.getAttribute('data-day') || '';
                        if (dateStr) {
                            results.push(dateStr);
                        }
                    }
                }
                
                return JSON.stringify(results);
            })();
        """.trimIndent()
    }
}
```

- [ ] **Step 3: 创建 MuniaoPlatformParser.kt**

木鸟民宿日历特点：使用 `.mu-calendar`、`.day-item` 等选择器，不可用状态通过 `.mu-disabled`、`.sold` 标记。

```kotlin
package com.housemonitor.service

class MuniaoPlatformParser : PlatformParser {
    override val platformId: String = "muniao"
    override val platformName: String = "木鸟民宿"

    override fun matchesUrl(url: String): Boolean {
        return url.contains("muniao.com") || url.contains("muniao")
    }

    override fun buildCalendarDetectionJs(): String {
        return """
            (function() {
                var results = [];
                var cells = document.querySelectorAll('.mu-calendar .day-item, .calendar-date, .day-cell, [data-date], [data-calendar-date]');
                var unavailableClasses = ['mu-disabled', 'sold', 'disabled', 'unavailable', 'booked', 'full', 'closed'];
                
                for (var i = 0; i < cells.length; i++) {
                    var cell = cells[i];
                    var className = cell.className || '';
                    var isUnavailable = false;
                    
                    for (var j = 0; j < unavailableClasses.length; j++) {
                        if (className.indexOf(unavailableClasses[j]) !== -1) {
                            isUnavailable = true;
                            break;
                        }
                    }
                    
                    if (!isUnavailable && cell.getAttribute('data-available') === 'false') {
                        isUnavailable = true;
                    }
                    if (!isUnavailable && cell.getAttribute('aria-disabled') === 'true') {
                        isUnavailable = true;
                    }
                    
                    if (isUnavailable) {
                        var dateStr = cell.getAttribute('data-date') || cell.getAttribute('data-calendar-date') || cell.getAttribute('data-day') || '';
                        if (dateStr) {
                            results.push(dateStr);
                        }
                    }
                }
                
                return JSON.stringify(results);
            })();
        """.trimIndent()
    }
}
```

- [ ] **Step 4: 更新 PlatformParserFactory，注册三个新 parser**

```kotlin
// PlatformParserFactory.kt
@Singleton
class PlatformParserFactory @Inject constructor() {
    private val parsers: List<PlatformParser> = listOf(
        MeituanPlatformParser(),
        TujiaPlatformParser(),
        XiaozhuPlatformParser(),
        MuniaoPlatformParser()
    )
    // ... 其余代码不变
}
```

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/housemonitor/service/TujiaPlatformParser.kt \
        app/src/main/java/com/housemonitor/service/XiaozhuPlatformParser.kt \
        app/src/main/java/com/housemonitor/service/MuniaoPlatformParser.kt \
        app/src/main/java/com/housemonitor/service/PlatformParserFactory.kt
git commit -m "feat: 新增途家/小猪民宿/木鸟民宿三个平台 Parser"
```

---

### Task D: 增强 PropertyMonitorWorker 变化检测逻辑

**Files:**
- Modify: `app/src/main/java/com/housemonitor/service/PropertyMonitorWorker.kt`

- [ ] **Step 1: 修改 monitorSingleProperty，计算并保存 changeSummary**

在获取 `unavailableDates` 和 `previousDates` 之后，保存记录之前，添加变化检测和 changeSummary 计算：

```kotlin
// 计算变化摘要
val changeSummary = computeChangeSummary(previousDates, unavailableDates)

// 保存监控记录（包含 changeSummary）
monitorRepository.saveMonitorRecord(
    propertyId = property.id,
    checkDate = checkDate,
    unavailableDates = unavailableDates,
    status = "success",
    changeSummary = changeSummary.toJson()
)
```

- [ ] **Step 2: 修改 MonitorRepository.saveMonitorRecord 支持 changeSummary**

```kotlin
suspend fun saveMonitorRecord(
    propertyId: String,
    checkDate: String,
    unavailableDates: List<String>,
    status: String = "success",
    changeSummary: String = ""
): Result<MonitorRecord> {
    return try {
        val record = MonitorRecord(
            propertyId = propertyId,
            checkDate = checkDate,
            unavailableDates = gson.toJson(unavailableDates),
            status = status,
            changeSummary = changeSummary
        )
        monitorRecordDao.insertRecord(record)
        Result.success(record)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

- [ ] **Step 3: 添加 computeChangeSummary 方法到 PropertyMonitorWorker**

```kotlin
private fun computeChangeSummary(
    previousDates: List<String>,
    currentDates: List<String>
): ChangeSummary {
    val newlyUnavailable = currentDates - previousDates.toSet()
    val newlyAvailable = previousDates - currentDates.toSet()

    return when {
        newlyUnavailable.isNotEmpty() && newlyAvailable.isNotEmpty() -> {
            ChangeSummary(newlyUnavailable, newlyAvailable, ChangeType.PARTIAL_CHANGE.name)
        }
        newlyUnavailable.isNotEmpty() -> {
            ChangeSummary(newlyUnavailable, emptyList(), ChangeType.BECAME_UNAVAILABLE.name)
        }
        newlyAvailable.isNotEmpty() -> {
            ChangeSummary(emptyList(), newlyAvailable, ChangeType.BECAME_AVAILABLE.name)
        }
        else -> ChangeSummary.noChange()
    }
}
```

- [ ] **Step 4: 保存后调用清理逻辑**

在保存记录并发送通知后，添加：
```kotlin
// 清理旧记录（每房源只保留最近 10 条）
monitorRepository.cleanupOldRecordsByPropertyId(property.id)
```

- [ ] **Step 5: 修改 checkForStatusChanges 使用 ChangeSummary**

将 `checkForStatusChanges` 方法改为接收 `ChangeSummary` 参数，而不是直接比较两个列表：

```kotlin
private suspend fun checkForStatusChanges(
    property: com.housemonitor.data.model.Property,
    changeSummary: ChangeSummary,
    userSettings: com.housemonitor.data.model.UserSettings?
) {
    if (userSettings?.notificationEnabled != true) return
    if (userSettings != null && isInQuietHours(userSettings)) return
    if (changeSummary.changeType == ChangeType.NO_CHANGE.name) return

    val changeType = when (changeSummary.changeType) {
        ChangeType.BECAME_UNAVAILABLE.name -> NotificationManager.StatusChangeType.BECAME_UNAVAILABLE
        ChangeType.BECAME_AVAILABLE.name -> NotificationManager.StatusChangeType.BECAME_AVAILABLE
        else -> NotificationManager.StatusChangeType.PARTIAL_CHANGE
    }
    val dates = changeSummary.newlyUnavailable + changeSummary.newlyAvailable

    notificationManager.showStatusChangeNotification(
        propertyName = property.name,
        changeType = changeType,
        dates = dates
    )
}
```

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "feat: 增强变化检测逻辑 - 计算 changeSummary + 每房源清理旧记录"
```

---

### Task E: 增强 NotificationManager 通知内容

**Files:**
- Modify: `app/src/main/java/com/housemonitor/util/NotificationManager.kt`

- [ ] **Step 1: 新增 showDetailedChangeNotification 方法**

```kotlin
/**
 * 显示详细变化通知（带变化摘要）
 */
fun showDetailedChangeNotification(
    propertyName: String,
    newlyUnavailable: List<String>,
    newlyAvailable: List<String>
) {
    val hasNewUnavailable = newlyUnavailable.isNotEmpty()
    val hasNewAvailable = newlyAvailable.isNotEmpty()

    if (!hasNewUnavailable && !hasNewAvailable) return

    val notificationId = Random.nextInt(1000, 9999)
    val title = "房源变化提醒"
    val contentText = buildString {
        append(propertyName).append("：")
        if (hasNewUnavailable) append("${newlyUnavailable.size}个日期变为无房")
        if (hasNewUnavailable && hasNewAvailable) append("，")
        if (hasNewAvailable) append("${newlyAvailable.size}个日期变为有房")
    }

    val intent = Intent(context, HistoryActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra("notification_property", propertyName)
    }

    val pendingIntent = PendingIntent.getActivity(
        context, notificationId, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(contentText)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setGroup(GROUP_KEY_PROPERTY_NOTIFICATIONS)
        .build()

    notificationManager.notify(notificationId, notification)
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/housemonitor/util/NotificationManager.kt
git commit -m "feat: 增强通知内容 - 显示变化摘要"
```

---

### Task F: 更新 AddPropertyDialog 支持新平台 + URL 自动检测

**Files:**
- Modify: `app/src/main/java/com/housemonitor/ui/property/AddPropertyDialog.kt`

- [ ] **Step 1: 添加平台列表常量**

```kotlin
private val platforms = listOf(
    "meituan" to "美团民宿",
    "tujia" to "途家",
    "xiaozhu" to "小猪民宿",
    "muniao" to "木鸟民宿"
)
```

- [ ] **Step 2: 添加 URL 自动检测逻辑**

```kotlin
// URL 变化时自动检测平台
LaunchedEffect(url) {
    val detected = platformParserFactory.detectPlatform(url)
    if (detected != selectedPlatform) {
        selectedPlatform = detected
    }
}
```

需要在 AddPropertyDialog 中添加 `@Inject` 的 `platformParserFactory`：

```kotlin
@Composable
fun AddPropertyDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String, description: String, platform: String) -> Unit,
    platformParserFactory: PlatformParserFactory = remember { PlatformParserFactory() }
)
```

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/housemonitor/ui/property/AddPropertyDialog.kt
git commit -m "feat: 添加房源对话框支持新平台 + URL 自动检测"
```

---

### Task G: 更新 PropertyCard 显示新平台标签

**Files:**
- Modify: `app/src/main/java/com/housemonitor/ui/property/PropertyCard.kt`

- [ ] **Step 1: 更新 platformLabel 映射**

```kotlin
private fun getPlatformLabel(platform: String): String {
    return when (platform) {
        "meituan" -> "美团民宿"
        "tujia" -> "途家"
        "xiaozhu" -> "小猪民宿"
        "muniao" -> "木鸟民宿"
        else -> "其他"
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/housemonitor/ui/property/PropertyCard.kt
git commit -m "feat: PropertyCard 支持新平台标签显示"
```

---

### Task H: 重新设计 MonitorRecordCard — 时间轴 + 对比视图

**Files:**
- Modify: `app/src/main/java/com/housemonitor/ui/history/MonitorRecordCard.kt`

- [ ] **Step 1: 重写 MonitorRecordCard 为时间轴节点样式**

```kotlin
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
        // 时间轴节点
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 时间轴圆点和线
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
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
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
                            val dates = com.google.gson.Gson().fromJson(
                                record.unavailableDates,
                                Array<String>::class.java
                            ) ?: emptyArray()
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
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f)
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
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/housemonitor/ui/history/MonitorRecordCard.kt
git commit -m "feat: 重新设计 MonitorRecordCard - 时间轴 + 展开对比视图"
```

---

### Task I: 更新 HistoryViewModel 加载每房源最近 10 条记录

**Files:**
- Modify: `app/src/main/java/com/housemonitor/ui/history/HistoryViewModel.kt`
- Modify: `app/src/main/java/com/housemonitor/ui/history/HistoryScreen.kt`

- [ ] **Step 1: 修改 HistoryViewModel，支持每房源加载最近 10 条记录**

在 `HistoryUiState` 中添加按房源分组的记录：

```kotlin
data class HistoryUiState(
    val allProperties: List<Property> = emptyList(),
    val allRecords: List<MonitorRecord> = emptyList(),
    val recordsByProperty: Map<String, List<MonitorRecord>> = emptyMap(),
    val filteredRecords: List<MonitorRecord> = emptyList(),
    val selectedPropertyId: String? = null,
    val propertyName: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
```

修改 `loadData()` 方法，从每个房源获取最近 10 条记录：

```kotlin
private fun loadData() {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        try {
            val properties = propertyRepository.getActiveProperties().first()
            val allRecords = mutableListOf<MonitorRecord>()
            val recordsByProperty = mutableMapOf<String, List<MonitorRecord>>()

            properties.forEach { property ->
                val records = monitorRepository.getRecentRecordsByPropertyId(property.id, 10)
                recordsByProperty[property.id] = records
                allRecords.addAll(records)
            }

            _uiState.update {
                it.copy(
                    allProperties = properties,
                    allRecords = allRecords.sortedByDescending { r -> r.checkedAt },
                    recordsByProperty = recordsByProperty,
                    filteredRecords = allRecords.sortedByDescending { r -> r.checkedAt },
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }
}
```

- [ ] **Step 2: 修改 HistoryScreen 传递 propertyName 到 MonitorRecordCard**

在 HistoryScreen 中，渲染 MonitorRecordCard 时传入 propertyName：

```kotlin
MonitorRecordCard(
    record = record,
    isFirst = index == 0,
    propertyName = propertiesMap[record.propertyId]?.name ?: "未知房源"
)
```

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/housemonitor/ui/history/HistoryViewModel.kt \
        app/src/main/java/com/housemonitor/ui/history/HistoryScreen.kt
git commit -m "feat: HistoryViewModel 加载每房源最近 10 条记录"
```

---

### Task J: 更新版本号 + 打 tag 发布

**Files:**
- Modify: `app/build.gradle`

- [ ] **Step 1: 更新版本号**

```groovy
versionCode 4
versionName "1.2.0"
```

- [ ] **Step 2: 提交 + 打 tag**

```bash
git add app/build.gradle
git commit -m "chore: bump version to 1.2.0"
git tag v1.2.0
git push origin main --tags
```

- [ ] **Step 3: 等待 CI/CD 完成，验证 APK 上传**

```bash
gh run watch --exit-status
curl -s "https://shz.al/~houseMonitorLog2"
```
