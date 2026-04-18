# 美团房源日历监控APP实施计划

## 项目结构

```
house-monitor/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/housemonitor/
│   │   │   │   ├── data/
│   │   │   │   │   ├── database/
│   │   │   │   │   ├── model/
│   │   │   │   │   └── repository/
│   │   │   │   ├── di/
│   │   │   │   ├── service/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── main/
│   │   │   │   │   ├── property/
│   │   │   │   │   └── settings/
│   │   │   │   ├── util/
│   │   │   │   └── HouseMonitorApplication.kt
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

## 阶段1：项目基础搭建 (1-2天)

### 任务1.1：创建Android项目
- 设置Gradle构建配置
- 配置依赖项 (Jetpack Compose, Room, WorkManager, Hilt等)
- 配置ProGuard规则

### 任务1.2：数据模型设计
- 创建Property实体类
- 创建MonitorRecord实体类
- 创建UserSettings实体类
- 配置Room数据库

### 任务1.3：依赖注入配置
- 配置Hilt依赖注入
- 创建Application类
- 配置模块绑定

## 阶段2：核心功能实现 (3-4天)

### 任务2.1：数据库层实现
- 实现Room DAO接口
- 实现Repository模式
- 添加数据库迁移策略

### 任务2.2：美团小程序集成
- 实现WebView组件
- 添加小程序页面加载逻辑
- 实现日历状态检测算法
- 处理页面交互和JavaScript桥接

### 任务2.3：后台监控服务
- 实现WorkManager后台任务
- 创建房源状态检查服务
- 添加定时任务调度
- 处理Doze模式优化

## 阶段3：UI界面开发 (3-4天)

### 任务3.1：主界面开发
- 实现房源列表界面
- 添加房源卡片组件
- 实现添加/删除房源功能

### 任务3.2：房源详情界面
- 显示房源信息和监控状态
- 展示历史监控记录
- 提供手动检查功能

### 任务3.3：设置界面
- 监控参数配置
- 通知设置
- 应用信息管理

## 阶段4：通知系统实现 (2-3天)

### 任务4.1：本地通知系统
- 实现NotificationManager
- 创建通知渠道
- 设计通知样式
- 添加通知点击处理

### 任务4.2：通知逻辑
- 实现无房日期检测通知
- 添加通知去重机制
- 实现免打扰时段处理

## 阶段5：测试和优化 (2-3天)

### 任务5.1：单元测试
- 业务逻辑单元测试
- 数据库操作测试
- 工具类测试

### 任务5.2：集成测试
- UI界面测试
- 后台服务测试
- 通知系统测试

### 任务5.3：性能优化
- 内存使用优化
- 电池消耗优化
- 启动时间优化

## 详细任务分解

### 任务1.1：创建Android项目

#### 1.1.1 项目配置
- 创建新的Android项目
- 配置minSdkVersion=23, targetSdkVersion=33
- 设置Kotlin版本和Android Gradle Plugin版本

#### 1.1.2 依赖配置
```gradle
// app/build.gradle
implementation "androidx.core:core-ktx:1.12.0"
implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0"
implementation "androidx.activity:activity-compose:1.8.2"
implementation "androidx.compose.ui:ui:1.5.4"
implementation "androidx.compose.material3:material3:1.1.2"
implementation "androidx.room:room-runtime:2.6.1"
implementation "androidx.room:room-ktx:2.6.1"
implementation "androidx.hilt:hilt-work:1.1.0"
implementation "androidx.work:work-runtime-ktx:2.9.0"
implementation "com.google.dagger:hilt-android:2.48"
```

### 任务1.2：数据模型设计

#### 1.2.1 Property实体
```kotlin
@Entity(tableName = "properties")
data class Property(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
```

#### 1.2.2 MonitorRecord实体
```kotlin
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
    val checkDate: String, // yyyy-MM-dd格式
    val unavailableDates: String, // JSON格式存储日期列表
    val checkedAt: Long = System.currentTimeMillis()
)
```

### 任务2.2：美团小程序集成

#### 2.2.1 WebView组件
```kotlin
class MeituanWebView(context: Context) : WebView(context) {
    private var onPageLoaded: ((String) -> Unit)? = null
    
    fun setOnPageLoadedListener(listener: (String) -> Unit) {
        onPageLoaded = listener
    }
    
    init {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
        }
        
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { onPageLoaded?.invoke(it) }
            }
        }
    }
    
    fun evaluateCalendarStatus(): String {
        val jsCode = """
            (function() {
                // 检测日历状态的JavaScript代码
                var calendarCells = document.querySelectorAll('.calendar-cell');
                var unavailableDates = [];
                
                calendarCells.forEach(function(cell) {
                    if (cell.classList.contains('unavailable') || 
                        cell.getAttribute('data-available') === 'false') {
                        var date = cell.getAttribute('data-date');
                        if (date) unavailableDates.push(date);
                    }
                });
                
                return JSON.stringify(unavailableDates);
            })();
        """.trimIndent()
        
        return evaluateJavascript(jsCode) { result ->
            // 处理返回结果
        }
    }
}
```

### 任务2.3：后台监控服务

#### 2.3.1 WorkManager配置
```kotlin
@HiltWorker
class PropertyMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val propertyRepository: PropertyRepository,
    private val notificationManager: NotificationManager
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val properties = propertyRepository.getActiveProperties()
            
            properties.forEach { property ->
                checkPropertyAvailability(property)
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    private suspend fun checkPropertyAvailability(property: Property) {
        // 实现房源可用性检查逻辑
        val webView = MeituanWebView(applicationContext)
        webView.loadUrl(property.url)
        
        // 等待页面加载完成
        delay(5000) // 5秒等待页面加载
        
        val unavailableDates = webView.evaluateCalendarStatus()
        
        if (unavailableDates.isNotEmpty()) {
            notificationManager.showUnavailableDatesNotification(
                property.name,
                unavailableDates
            )
        }
    }
}
```

### 任务3.1：主界面开发

#### 3.1.1 房源列表界面
```kotlin
@Composable
fun PropertyListScreen(
    viewModel: PropertyListViewModel = hiltViewModel(),
    onAddProperty: () -> Unit = {},
    onPropertyClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("房源监控") },
                actions = {
                    IconButton(onClick = onAddProperty) {
                        Icon(Icons.Default.Add, contentDescription = "添加房源")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.properties) { property ->
                PropertyCard(
                    property = property,
                    onClick = { onPropertyClick(property.id) },
                    onToggleActive = { viewModel.togglePropertyActive(property.id) }
                )
            }
        }
    }
}
```

### 任务4.1：本地通知系统

#### 4.1.1 NotificationManager
```kotlin
@Singleton
class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "房源监控通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "房源无房日期提醒"
                enableVibration(true)
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showUnavailableDatesNotification(propertyName: String, dates: List<String>) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("房源无房提醒")
            .setContentText("$propertyName 在以下日期无房: ${dates.joinToString(", ")}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(generateNotificationId(), notification)
    }
    
    companion object {
        private const val CHANNEL_ID = "property_monitor_channel"
    }
}
```

## 测试计划

### 单元测试
- PropertyRepository测试
- MonitorService测试
- NotificationManager测试

### 集成测试
- 完整的房源监控流程测试
- 通知系统测试
- UI界面交互测试

### 性能测试
- 内存泄漏测试
- 电池消耗测试
- 网络请求性能测试

## 风险分析

### 技术风险
1. 美团小程序页面结构变化
2. 安卓系统权限限制
3. 后台服务限制

### 应对策略
1. 实现页面结构自适应检测
2. 提供多种监控方式选择
3. 优化后台服务策略

## 交付物

1. 完整的Android应用源代码
2. 单元测试和集成测试代码
3. 应用安装包(APK)
4. 技术文档和用户手册