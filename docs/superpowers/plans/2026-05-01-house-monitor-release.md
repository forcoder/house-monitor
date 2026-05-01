# 房源监控 APP 多平台支持 + GitHub 发布计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为房源监控 APP 添加多平台支持（重点美团民宿），推送到 GitHub 并改为 Public，配置 GitHub Actions 自动构建发布 APK，提供下载链接。

**Architecture:**
- 数据层：Property 表新增 platform 字段，支持多平台标识
- 解析层：抽取 PlatformParser 接口，不同平台实现各自的 JS 检测逻辑
- CI/CD：GitHub Actions 自动构建 debug APK 并作为 release artifact 上传
- 发布：GitHub repo 改为 public，Actions 构建产物可直接下载

**Tech Stack:** Kotlin, Jetpack Compose, Room, WorkManager, Hilt, GitHub Actions

---

### Task 1: 修改 Property 数据模型 — 新增 platform 字段

**Files:**
- Modify: `app/src/main/java/com/housemonitor/data/model/Property.kt`
- Modify: `app/src/main/java/com/housemonitor/data/database/HouseMonitorDatabase.kt`

- [ ] **Step 1: 修改 Property 实体，新增 platform 字段**

```kotlin
package com.housemonitor.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "properties")
data class Property(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val platform: String = "meituan",  // 新增：平台标识 meituan/tujia/fliggy/other
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastCheckedAt: Long = 0L,
    val description: String = ""
)
```

- [ ] **Step 2: 修改数据库版本号，添加迁移**

在 `HouseMonitorDatabase.kt` 中：
- 将数据库 version 从当前值 +1
- 添加 Migration 处理新增 platform 字段（默认值 "meituan"）

```kotlin
// 假设当前 version = 1，改为 version = 2
@Database(
    entities = [Property::class, MonitorRecord::class, UserSettings::class],
    version = 2,  // +1
    exportSchema = true
)
abstract class HouseMonitorDatabase : RoomDatabase() {
    // ... existing code

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE properties ADD COLUMN platform TEXT NOT NULL DEFAULT 'meituan'"
                )
            }
        }
    }
}
```

在 database builder 中添加 `.addMigrations(MIGRATION_1_2)`

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/housemonitor/data/model/Property.kt app/src/main/java/com/housemonitor/data/database/HouseMonitorDatabase.kt
git commit -m "feat: add platform field to Property model with Room migration"
```

---

### Task 2: 创建 PlatformParser 接口和实现

**Files:**
- Create: `app/src/main/java/com/housemonitor/service/PlatformParser.kt`
- Create: `app/src/main/java/com/housemonitor/service/MeituanPlatformParser.kt`

- [ ] **Step 1: 创建 PlatformParser 接口**

```kotlin
package com.housemonitor.service

/**
 * 平台解析器接口 — 不同平台实现各自的日历状态检测逻辑
 */
interface PlatformParser {
    /** 平台标识 */
    val platformId: String

    /** 平台显示名称 */
    val platformName: String

    /** 生成注入到 WebView 的 JS 代码，用于检测无房日期 */
    fun buildCalendarDetectionJs(): String

    /** 平台匹配的 URL 规则（用于自动识别平台） */
    fun matchesUrl(url: String): Boolean
}
```

- [ ] **Step 2: 创建美团民宿解析器（从现有 MeituanWebView 中提取 JS 逻辑）**

```kotlin
package com.housemonitor.service

class MeituanPlatformParser : PlatformParser {
    override val platformId: String = "meituan"
    override val platformName: String = "美团民宿"

    override fun matchesUrl(url: String): Boolean {
        return url.contains("meituan.com") || url.contains("meituan")
    }

    override fun buildCalendarDetectionJs(): String {
        return """
            (function() {
                try {
                    var calendarElements = [];
                    var selectors = [
                        '.calendar-cell', '.calendar-item', '[data-calendar]',
                        '.date-cell', '.day-cell', '[class*="calendar"]', '[class*="date"]'
                    ];
                    selectors.forEach(function(selector) {
                        var elements = document.querySelectorAll(selector);
                        if (elements.length > 0) {
                            calendarElements = Array.from(elements);
                            return;
                        }
                    });
                    if (calendarElements.length === 0) {
                        var allElements = document.querySelectorAll('*');
                        calendarElements = Array.from(allElements).filter(function(el) {
                            var text = el.textContent || '';
                            return /\d{4}-\d{2}-\d{2}/.test(text) ||
                                   /\d{1,2}[月日]/.test(text) ||
                                   el.classList.toString().includes('date') ||
                                   el.classList.toString().includes('calendar');
                        });
                    }
                    var unavailableDates = [];
                    var today = new Date();
                    calendarElements.forEach(function(element) {
                        var isUnavailable = false;
                        var dateStr = '';
                        if (element.classList.contains('unavailable') ||
                            element.classList.contains('disabled') ||
                            element.classList.contains('occupied') ||
                            element.classList.contains('booked') ||
                            element.classList.contains('full')) {
                            isUnavailable = true;
                        }
                        if (element.getAttribute('data-available') === 'false' ||
                            element.getAttribute('data-status') === 'unavailable' ||
                            element.getAttribute('disabled') !== null) {
                            isUnavailable = true;
                        }
                        var style = window.getComputedStyle(element);
                        if (style.color === 'rgb(128, 128, 128)' ||
                            style.color === 'rgb(153, 153, 153)' ||
                            style.opacity === '0.5') {
                            isUnavailable = true;
                        }
                        dateStr = element.getAttribute('data-date') ||
                                 element.getAttribute('data-day') ||
                                 element.textContent.trim();
                        if (dateStr && isUnavailable) {
                            var parsedDate = parseDate(dateStr, today);
                            if (parsedDate) unavailableDates.push(parsedDate);
                        }
                    });
                    return JSON.stringify(unavailableDates);
                } catch (error) {
                    return JSON.stringify([]);
                }
            })();
            function parseDate(dateStr, today) {
                try {
                    var patterns = [
                        /(\d{4})-(\d{1,2})-(\d{1,2})/,
                        /(\d{1,2})[月\/\\-](\d{1,2})[日]?/
                    ];
                    for (var i = 0; i < patterns.length; i++) {
                        var match = dateStr.match(patterns[i]);
                        if (match) {
                            if (i === 0) return match[0];
                            var month = parseInt(match[1]);
                            var day = parseInt(match[2]);
                            var year = today.getFullYear();
                            if (month < today.getMonth() + 1) year++;
                            return year + '-' + month.toString().padStart(2, '0') + '-' + day.toString().padStart(2, '0');
                        }
                    }
                    var date = new Date(dateStr);
                    if (!isNaN(date.getTime())) {
                        return date.getFullYear() + '-' + (date.getMonth()+1).toString().padStart(2,'0') + '-' + date.getDate().toString().padStart(2,'0');
                    }
                    return null;
                } catch(e) { return null; }
            }
        """.trimIndent()
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/housemonitor/service/PlatformParser.kt app/src/main/java/com/housemonitor/service/MeituanPlatformParser.kt
git commit -m "feat: add PlatformParser interface and Meituan implementation"
```

---

### Task 3: 创建 PlatformParserFactory 并在 MeituanWebView 中集成

**Files:**
- Create: `app/src/main/java/com/housemonitor/service/PlatformParserFactory.kt`
- Modify: `app/src/main/java/com/housemonitor/service/MeituanWebView.kt`
- Modify: `app/src/main/java/com/housemonitor/service/PropertyMonitorWorker.kt`

- [ ] **Step 1: 创建 PlatformParserFactory**

```kotlin
package com.housemonitor.service

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlatformParserFactory @Inject constructor() {
    private val parsers: List<PlatformParser> = listOf(
        MeituanPlatformParser()
        // 未来扩展：TujiaPlatformParser(), FliggyPlatformParser()
    )

    fun getParser(platformId: String): PlatformParser {
        return parsers.find { it.platformId == platformId }
            ?: parsers.first() // 默认回退到美团
    }

    fun detectPlatform(url: String): String {
        return parsers.find { it.matchesUrl(url) }?.platformId ?: "meituan"
    }

    fun getAllPlatforms(): List<PlatformParser> = parsers
}
```

- [ ] **Step 2: 修改 MeituanWebView，使用 PlatformParser 获取 JS 代码**

在 `evaluateCalendarStatus()` 方法中，将原来硬编码的 JS 字符串改为从 parser 获取。同时重构 MeituanWebView 使其接收 PlatformParser 参数：

```kotlin
class MeituanWebView(
    private val context: Context,
    private val platformParser: PlatformParser
) {
    // ... 其他代码不变

    suspend fun evaluateCalendarStatus(): List<String> {
        return suspendCancellableCoroutine { continuation ->
            // 使用 platformParser 获取 JS 代码
            val jsCode = platformParser.buildCalendarDetectionJs()
            // ... 其余 evaluateJavascript 逻辑不变
        }
    }
}
```

- [ ] **Step 3: 修改 PropertyMonitorWorker，注入 PlatformParserFactory**

在 `monitorSingleProperty` 中：
```kotlin
// 替换原来的 MeituanWebView(applicationContext) 为：
val parser = platformParserFactory.getParser(property.platform)
val meituanWebView = MeituanWebView(applicationContext, parser)
```

需要在 Worker 的构造参数中添加 `platformParserFactory: PlatformParserFactory`（通过 Hilt 注入）。

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/housemonitor/service/PlatformParserFactory.kt app/src/main/java/com/housemonitor/service/MeituanWebView.kt app/src/main/java/com/housemonitor/service/PropertyMonitorWorker.kt
git commit -m "feat: integrate PlatformParser into WebView and Worker via factory"
```

---

### Task 4: 更新 AddPropertyDialog — 添加平台选择

**Files:**
- Modify: `app/src/main/java/com/housemonitor/ui/property/AddPropertyDialog.kt`
- Modify: `app/src/main/java/com/housemonitor/ui/main/MainViewModel.kt`

- [ ] **Step 1: 修改 AddPropertyDialog，添加平台选择下拉菜单**

在 `AddPropertyDialog` Composable 中，在房源名称输入框之后添加平台选择：

```kotlin
// 在 name 输入框之后、url 输入框之前添加：
var selectedPlatform by remember { mutableStateOf("meituan") }
var platformDropdownExpanded by remember { mutableStateOf(false) }

val platformOptions = listOf(
    "meituan" to "美团民宿",
    "other" to "其他平台"
)

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
```

修改 `onConfirm` 回调签名，增加 platform 参数：
```kotlin
onConfirm: (String, String, String, String) -> Unit  // name, url, description, platform
```

调用时传入 `selectedPlatform`：
```kotlin
onConfirm(name.trim(), url.trim(), description.trim(), selectedPlatform)
```

- [ ] **Step 2: 修改 MainViewModel.addProperty，接收 platform 参数**

```kotlin
fun addProperty(name: String, url: String, description: String, platform: String = "meituan") {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val property = Property(
            name = name,
            url = url,
            description = description,
            platform = platform,
            isActive = true
        )
        propertyRepository.addProperty(property)
        loadProperties()
        workManagerService.scheduleImmediateMonitoring(property.id)
    }
}
```

- [ ] **Step 3: 修改 MainScreen 中调用 AddPropertyDialog 的地方**

```kotlin
AddPropertyDialog(
    onDismiss = { showAddDialog = false },
    onConfirm = { name, url, description, platform ->
        viewModel.addProperty(name, url, description, platform)
        showAddDialog = false
    }
)
```

- [ ] **Step 4: 在 PropertyCard 中显示平台标签**

在 `PropertyCard` 的房源名称下方添加平台标签：

```kotlin
// 在 name Text 之后添加
val platformLabel = when (property.platform) {
    "meituan" -> "美团民宿"
    else -> "其他"
}
Text(
    text = platformLabel,
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.primary
)
```

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/housemonitor/ui/property/AddPropertyDialog.kt app/src/main/java/com/housemonitor/ui/main/MainViewModel.kt app/src/main/java/com/housemonitor/ui/main/MainScreen.kt app/src/main/java/com/housemonitor/ui/property/PropertyCard.kt
git commit -m "feat: add platform selector to property add dialog and display in card"
```

---

### Task 5: 配置 GitHub Actions 自动构建发布

**Files:**
- Modify: `.github/workflows/android-build.yml`

- [ ] **Step 1: 更新 GitHub Actions 工作流，添加 release 构建和 APK 发布**

```yaml
name: Android Build & Release

on:
  push:
    branches: [ main, master ]
  pull_request:
    branches: [ main, master ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v5

    - name: Set up JDK 17
      uses: actions/setup-java@v5
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build Debug APK
      run: ./gradlew assembleDebug --no-daemon

    - name: Upload Debug APK as Artifact
      uses: actions/upload-artifact@v5
      with:
        name: house-monitor-debug
        path: app/build/outputs/apk/debug/app-debug.apk
        retention-days: 90

  release:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'

    steps:
    - uses: actions/checkout@v5

    - name: Set up JDK 17
      uses: actions/setup-java@v5
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build Release APK
      run: ./gradlew assembleRelease --no-daemon

    - name: Get version
      id: version
      run: |
        VERSION=$(grep -oP 'versionName "\K[^"]+' app/build.gradle)
        echo "version=$VERSION" >> $GITHUB_OUTPUT
        echo "VERSION=$VERSION" >> $GITHUB_ENV

    - name: Create GitHub Release
      uses: softprops/action-gh-release@v2
      with:
        tag_name: v${{ steps.version.outputs.version }}.${{ github.run_number }}
        name: 房源监控 v${{ steps.version.outputs.version }} (Build ${{ github.run_number }})
        body: |
          ## 房源监控 APP 更新
          - 支持多平台房源监控（美团民宿优先）
          - 状态变化实时通知
          - 完整历史记录查看
        files: app/build/outputs/apk/release/app-release.apk
        draft: false
        prerelease: false
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

- [ ] **Step 2: 确保 app/build.gradle 中 release 签名配置可用**

在 `app/build.gradle` 的 `buildTypes.release` 中添加 debug 签名（用于 CI 构建）：

```groovy
android {
    // ... existing code

    signingConfigs {
        debug {
            storeFile file('debug.keystore')
            storePassword 'android'
            keyAlias 'androiddebugkey'
            keyPassword 'android'
        }
    }

    buildTypes {
        debug {
            signingConfig signingConfigs.debug
        }
        release {
            signingConfig signingConfigs.debug  // CI 使用 debug 签名
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add .github/workflows/android-build.yml app/build.gradle
git commit -m "ci: configure GitHub Actions for debug and release APK builds"
```

---

### Task 6: 推送到 GitHub 并设置仓库为 Public

**Files:** (无文件修改，纯命令行操作)

- [ ] **Step 1: 确保所有更改已提交**

```bash
git status
# 确认 working tree clean
```

- [ ] **Step 2: 推送到 GitHub**

```bash
git push origin main
```

- [ ] **Step 3: 将仓库改为 Public**

```bash
gh repo edit --visibility public
```

- [ ] **Step 4: 触发 GitHub Actions 构建**

推送后 Actions 会自动触发。确认构建状态：

```bash
gh run list --limit 5
```

等待构建完成后获取下载链接：

```bash
gh release list
```

- [ ] **Step 5: 提供下载链接**

构建成功后，APK 下载地址为：
```
https://github.com/forcoder/house-monitor/releases/latest/download/app-release.apk
```

以及最新构建的 artifact 可在 Actions 页面获取。

---

### Task 7: 更新版本号和最终验证

**Files:**
- Modify: `app/build.gradle`

- [ ] **Step 1: 更新应用版本号**

```groovy
defaultConfig {
    applicationId "com.housemonitor"
    minSdk 23
    targetSdk 33
    versionCode 2
    versionName "1.1.0"
    // ...
}
```

- [ ] **Step 2: 最终提交和推送**

```bash
git add app/build.gradle
git commit -m "chore: bump version to 1.1.0"
git push origin main
```

- [ ] **Step 3: 等待 Actions 构建完成，确认 release 页面有 APK 下载**

```bash
# 等待几分钟后检查
gh run list --limit 3
gh release view
```
