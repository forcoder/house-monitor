# 🏠 美团房源监控APP

[![Android CI/CD](https://github.com/forcoder/house-monitor/actions/workflows/android-build.yml/badge.svg)](https://github.com/forcoder/house-monitor/actions/workflows/android-build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)

一个智能的安卓APP，用于自动监控美团小程序中的房源日历。当发现无房日期时，会通过系统推送通知及时提醒你。

## ✨ 功能特点

### 🎯 核心功能
- **自动监控**: 每5分钟检查一次房源可用性
- **智能提醒**: 发现无房日期时发送系统通知
- **后台运行**: 使用Service确保监控持续运行
- **开机自启**: 设备重启后自动恢复监控
- **状态显示**: 实时显示监控状态和最后检查时间

### 🛠 技术特性
- **MVVM架构**: 清晰的代码结构，便于维护
- **Material Design 3**: 现代化的用户界面
- **Kotlin协程**: 高效的异步处理
- **OkHttp**: 稳定的网络请求
- **WorkManager**: 可靠的后台任务管理

## 📱 应用截图

> *截图占位符 - 实际使用请添加应用截图*

| 主界面 | 监控状态 | 通知提醒 |
|--------|----------|----------|
| ![主界面](screenshots/main.png) | ![监控状态](screenshots/status.png) | ![通知提醒](screenshots/notification.png) |

## 🚀 快速开始

### 方法一：下载预构建APK（推荐）

1. 进入 [Releases页面](https://github.com/forcoder/house-monitor/releases)
2. 下载最新版本的 `app-debug.apk`
3. 在安卓设备上安装APK
4. 打开应用并开始使用

### 方法二：使用GitHub Actions构建

1. Fork本项目到你的GitHub账户
2. 启用GitHub Actions
3. 推送代码触发自动构建
4. 从Actions下载构建的APK

### 方法三：本地构建

#### 环境要求
- Android Studio Giraffe 或更高版本
- JDK 8 或 JDK 17
- Android SDK API Level 34

#### 构建步骤
1. 克隆仓库：
   ```bash
   git clone https://github.com/forcoder/house-monitor.git
   cd house-monitor
   ```

2. 使用Android Studio打开项目
3. 等待Gradle同步完成
4. 点击 "Build" → "Build APK(s)"

## 📖 使用说明

### 基础使用
1. **输入地址**: 在美团小程序中找到房源并复制地址
2. **开始监控**: 点击"开始监控"按钮
3. **等待提醒**: APP自动每5分钟检查房源状态
4. **接收通知**: 发现无房日期时收到系统通知

### 高级功能
- **后台运行**: APP会在后台持续监控
- **状态查看**: 主界面显示监控状态和最后检查时间
- **停止监控**: 随时可以停止监控服务

## 🛠 开发指南

### 项目结构
```
house-monitor/
├── app/
│   ├── src/main/java/com/workbuddy/house/monitor/
│   │   ├── MainActivity.kt          # 主界面
│   │   ├── viewmodel/               # 业务逻辑层
│   │   ├── service/                 # 后台服务
│   │   ├── model/                   # 数据模型
│   │   ├── receiver/                # 广播接收器
│   │   └── util/                    # 工具类
│   └── build.gradle.kts            # 应用构建配置
├── .github/workflows/              # GitHub Actions配置
├── build.gradle.kts                # 项目构建配置
└── README.md                       # 项目文档
```

### 核心类说明
- **MainActivity**: 用户主界面，处理用户交互
- **MainViewModel**: 业务逻辑处理，状态管理
- **MonitoringService**: 后台监控服务
- **MtMeituanMonitor**: 美团API调用和数据解析
- **NotificationHelper**: 通知管理
- **PreferencesHelper**: 数据持久化

### 依赖库
```kotlin
// 核心Android库
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

// Compose UI
implementation(platform("androidx.compose:compose-bom:2023.10.01"))
implementation("androidx.compose.material3:material3")

// 网络请求
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// 后台任务
implementation("androidx.work:work-runtime-ktx:2.9.0")
```

## 🔧 配置说明

### 修改监控频率
编辑 `MonitoringService.kt`：
```kotlin
delay(5 * 60 * 1000) // 修改此值改变检查间隔（毫秒）
```

### 自定义通知
编辑 `NotificationHelper.kt`：
- 修改通知标题和内容
- 调整通知优先级和样式
- 自定义通知声音和振动

### API适配
编辑 `MtMeituanMonitor.kt`：
- 根据实际美团API调整接口调用
- 修改数据解析逻辑
- 添加错误处理机制

## 📊 版本历史

### v1.0.0 (2026-04-14)
- 🎉 首次发布
- ✅ 实现基础监控功能
- ✅ 添加系统通知
- ✅ 支持后台运行
- ✅ 添加开机自启

### 计划功能
- [ ] 多房源同时监控
- [ ] 自定义监控频率
- [ ] 历史记录功能
- [ ] 价格监控功能
- [ ] 数据导出功能
- [ ] 深色主题支持

## 🤝 贡献指南

我们欢迎所有形式的贡献！请阅读以下指南：

### 贡献流程
1. Fork本项目
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建Pull Request

### 代码规范
- 遵循 [Kotlin编码规范](https://kotlinlang.org/docs/coding-conventions.html)
- 使用有意义的变量和函数名
- 添加必要的注释
- 保持代码简洁和可读性

### 提交信息规范
- 使用清晰的提交信息
- 遵循 [约定式提交](https://www.conventionalcommits.org/)
- 示例：`feat: 添加新功能`、`fix: 修复bug`、`docs: 更新文档`

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 🔒 隐私和安全

### 数据收集
- 本应用不会收集任何个人数据
- 所有配置数据仅存储在本地设备
- 不会上传任何用户信息到服务器

### 网络请求
- 仅向美团小程序API发送房源查询请求
- 不会向第三方服务器发送任何数据
- 所有网络请求都通过HTTPS进行

### 权限说明
- `INTERNET`: 访问美团API
- `POST_NOTIFICATIONS`: 发送系统通知
- `RECEIVE_BOOT_COMPLETED`: 开机自启动
- `FOREGROUND_SERVICE`: 前台服务权限

## ❓ 常见问题

### Q: 为什么监控不工作？
A: 请检查以下几点：
1. 确保输入的美团小程序地址正确
2. 检查网络连接是否正常
3. 确认已允许应用发送通知
4. 查看应用是否有后台运行权限

### Q: 如何自定义监控频率？
A: 目前需要在代码中修改 `MonitoringService.kt` 的延迟时间。未来版本将支持在UI中配置。

### Q: 应用耗电量大吗？
A: 应用采用优化的后台服务，每5分钟检查一次，耗电量很小。建议保持应用运行以获得最佳体验。

### Q: 支持哪些安卓版本？
A: 支持Android 7.0 (API 24) 及以上版本。

## 📞 联系我们

### 问题反馈
- 提交 [GitHub Issue](https://github.com/forcoder/house-monitor/issues)
- 描述问题详细情况
- 提供设备信息和安卓版本

### 功能建议
- 在Issues中提交功能建议
- 描述使用场景和期望功能
- 参与社区讨论

### 技术讨论
- 加入开发者讨论
- 分享使用经验
- 提供改进建议

---

**🌟 如果这个项目对你有帮助，请给它一个Star！**

**💝 你的支持是我们持续改进的动力！**