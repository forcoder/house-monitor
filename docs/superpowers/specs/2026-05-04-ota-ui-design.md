# 房源监控 APP — OTA UI 功能设计文档

## 目标
将已实现的 OTA 后端能力（OtaManager）接入 UI 层，提供手动检查更新和自动检查更新功能。

## 架构

```
SettingsScreen → SettingsViewModel → OtaManager → OtaRepository → ShzApiService → shz.al
                                  ↕ StateFlow<UpdateStatus>
                            UpdateDialog / DownloadProgressDialog / InstallPromptDialog
```

## 功能需求

### 1. 手动更新检查
- 设置页"检查更新"按钮触发检查
- 检查中显示 loading 状态
- 发现新版本 → 弹出 UpdateDialog
- 无新版本 → Toast "已是最新版本"
- 检查失败 → Toast 显示错误信息

### 2. 自动更新检查
- **时机1**：每次进入设置页时自动检查（如果距上次检查超过1小时）
- **时机2**：每天首次打开APP时自动检查（用 SharedPreferences 记录日期，同一天不重复）
- 检查到新版本 → 直接弹窗提示（UpdateDialog）
- 强制更新（forceUpdate=true）→ 弹窗不可关闭，必须更新才能继续使用

### 3. UI 组件

#### UpdateDialog（发现新版本弹窗）
- 标题：发现新版本 v{versionName}
- 内容：更新日志（releaseNotes）
- 按钮：立即更新 / 稍后
- 强制更新模式：只显示"立即更新"按钮，不可取消

#### DownloadProgressDialog（下载进度弹窗）
- 标题：正在下载更新
- 内容：进度条（0-100%）、已下载/总大小
- 按钮：取消下载

#### InstallPromptDialog（安装提示弹窗）
- 标题：下载完成
- 内容：新版本已下载完成，是否立即安装？
- 按钮：立即安装 / 稍后

### 4. 设置页状态显示
- 默认：显示"检查更新 · 当前版本 v1.2.0"
- 检查中：显示 loading 指示器
- 有新版本：显示"有新版本可用"badge + 红点
- 下载中：显示下载进度

## 数据流

```
SettingsViewModel.inject(OtaManager)
  ↓ collect updateStatus StateFlow
SettingsScreen 观察状态 → 显示对应 UI
  ↓ 用户点击检查更新
SettingsViewModel.checkForUpdate()
  ↓ OtaManager.checkForUpdate()
  ↓ 发现新版本 → _updateStatus = UPDATE_AVAILABLE
SettingsScreen 检测到 UPDATE_AVAILABLE → 显示 UpdateDialog
  ↓ 用户点"立即更新"
SettingsViewModel.startDownload(update)
  ↓ OtaManager.startDownload(update)
  ↓ downloadProgress StateFlow 更新
DownloadProgressDialog 显示进度
  ↓ 下载完成 → _updateStatus = DOWNLOADED
SettingsScreen 显示 InstallPromptDialog
  ↓ 用户点"立即安装"
SettingsViewModel.triggerInstall()
  ↓ OtaManager.triggerInstall() → 系统安装器
```

## 文件变更

| 文件 | 变更 |
|------|------|
| SettingsViewModel.kt | 注入 OtaManager，添加 checkForUpdate/startDownload/triggerInstall/cancelDownload 方法，添加 autoCheck 逻辑 |
| SettingsScreen.kt | 接入 updateStatus 状态，添加 UpdateDialog/DownloadProgressDialog/InstallPromptDialog |
| strings.xml | 添加 OTA 相关字符串 |
| MainActivity.kt | 添加自动检查逻辑（每天首次打开） |

## 版本计划
- versionCode: 5
- versionName: "1.3.0"
