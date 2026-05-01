# 美团房源日历监控APP

一个智能的安卓APP，用于自动监控美团民宿小程序中的房源日历。当发现无房日期时，会通过系统推送通知及时提醒用户。

## 功能特性

### 🏠 房源管理
- 添加多个房源监控
- 支持美团民宿小程序链接
- 房源状态管理（启用/暂停监控）
- 房源信息编辑和删除

### 🔍 智能监控
- 自动检测房源日历状态
- 识别无房日期
- 可配置监控频率（默认60分钟）
- 后台持续监控

### 📱 智能通知
- 无房日期推送通知
- 免打扰时段设置
- 通知历史记录
- 点击通知快速查看详情

### ⚙️ 个性化设置
- 监控间隔自定义
- 通知开关控制
- 免打扰时间设置
- 自动刷新配置

## 技术架构

### 前端技术
- **框架**: Android原生开发 (Kotlin)
- **UI框架**: Jetpack Compose
- **架构模式**: MVVM (Model-View-ViewModel)
- **依赖注入**: Hilt

### 后端服务
- **数据库**: Room Persistence Library
- **网络请求**: 美团小程序WebView集成
- **后台任务**: WorkManager
- **异步处理**: Kotlin Coroutines + Flow

### 核心组件
- **MeituanWebView**: 美团小程序集成组件
- **PropertyMonitorWorker**: 后台监控服务
- **NotificationManager**: 通知管理系统
- **Repository模式**: 数据访问层

## 项目结构

```
house-monitor/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/housemonitor/
│   │   │   │   ├── data/                 # 数据层
│   │   │   │   │   ├── database/         # Room数据库
│   │   │   │   │   ├── model/           # 数据实体
│   │   │   │   │   └── repository/      # 数据仓库
│   │   │   │   ├── di/                 # 依赖注入
│   │   │   │   ├── service/            # 服务层
│   │   │   │   ├── ui/                 # UI层
│   │   │   │   │   ├── main/           # 主界面
│   │   │   │   │   ├── property/       # 房源界面
│   │   │   │   │   └── settings/      # 设置界面
│   │   │   │   ├── util/               # 工具类
│   │   │   │   └── HouseMonitorApplication.kt
│   │   │   ├── res/                 # 资源文件
│   │   │   └── AndroidManifest.xml
│   │   └── test/                  # 测试代码
│   └── build.gradle
├── gradle/
│   └── wrapper/                  # Gradle Wrapper
├── build.gradle
├── settings.gradle
└── gradlew                      # Gradle启动脚本
```

## 快速开始

### 环境要求
- Android Studio Hedgehog 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.2

### 构建步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd house-monitor
```

2. **使用Android Studio打开**
   - 打开Android Studio
   - 选择 "Open an existing Android Studio project"
   - 选择 `house-monitor` 目录

3. **同步Gradle依赖**
   - 等待Android Studio自动同步Gradle依赖
   - 或者手动点击 "Sync Project with Gradle Files"

4. **构建项目**
```bash
./gradlew build
```

5. **运行应用**
   - 连接Android设备或启动模拟器
   - 点击 "Run" 按钮或执行：
```bash
./gradlew installDebug
```

## 使用说明

### 添加房源
1. 打开APP，点击右下角 "+" 按钮
2. 输入房源名称和美团小程序链接
3. 点击 "添加" 完成房源添加

### 配置监控
1. 进入 "设置" 页面
2. 调整监控间隔（15分钟-24小时）
3. 设置免打扰时段
4. 开启/关闭通知

### 查看监控状态
- 主界面显示所有房源及其监控状态
- 绿色标识表示正在监控
- 灰色标识表示已暂停

## API说明

### 美团小程序集成
APP通过WebView加载美团民宿小程序，使用JavaScript检测日历状态：

```javascript
// 检测无房日期的算法
document.querySelectorAll('.calendar-cell, .date-cell, [data-calendar]')
```

### 数据模型

#### Property (房源)
```kotlin
data class Property(
    val id: String,
    val name: String,
    val url: String,
    val isActive: Boolean,
    val createdAt: Long
)
```

#### MonitorRecord (监控记录)
```kotlin
data class MonitorRecord(
    val id: String,
    val propertyId: String,
    val checkDate: String,
    val unavailableDates: String, // JSON格式
    val checkedAt: Long
)
```

## 配置说明

### 监控间隔设置
- 最小间隔：15分钟
- 默认间隔：60分钟
- 最大间隔：24小时

### 通知设置
- 推送通知：默认开启
- 免打扰时段：默认22:00-08:00
- 通知渠道：房源监控通知

## 性能优化

### 电池优化
- 使用WorkManager进行智能调度
- 支持Doze模式
- 低功耗后台运行

### 内存优化
- 使用Flow进行数据流管理
- 及时释放WebView资源
- 数据库查询优化

## 安全考虑

### 数据安全
- 本地数据加密存储
- 不收集用户隐私信息
- 符合GDPR隐私规范

### 应用安全
- HTTPS通信
- WebView安全配置
- 权限最小化原则

## 测试指南

### 单元测试
```bash
./gradlew test
```

### 集成测试
```bash
./gradlew connectedAndroidTest
```

### 手动测试场景
1. 添加房源并验证监控功能
2. 测试通知推送功能
3. 验证后台服务持续性
4. 测试不同网络条件下的表现

## 常见问题

### Q: 为什么收不到通知？
A: 请检查：
- 通知权限是否开启
- 是否在免打扰时段
- 系统电池优化设置

### Q: 监控不准确怎么办？
A: 请尝试：
- 重新添加房源链接
- 手动刷新房源状态
- 检查网络连接

### Q: 应用耗电量大吗？
A: APP采用低功耗设计：
- 使用系统WorkManager调度
- 智能检测网络状态
- 支持系统省电模式

## 版本历史

### v1.0.0 (2026-04-18)
- 初始版本发布
- 基础房源监控功能
- 智能通知系统
- 个性化设置

## 贡献指南

欢迎提交Issue和Pull Request！

### 开发流程
1. Fork项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建Pull Request

### 代码规范
- 遵循Kotlin编码规范
- 使用Android官方架构指南
- 编写单元测试
- 保持代码简洁清晰

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 联系方式

如有问题或建议，请通过以下方式联系：
- 邮箱: [your-email@example.com]
- GitHub: [项目地址]

---

**注意**: 本应用仅供学习和研究使用，使用美团小程序接口时请遵守相关服务条款。