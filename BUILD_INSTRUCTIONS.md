# APK构建说明

## 方法一：使用Android Studio (推荐)

### 步骤1：打开项目
1. 启动Android Studio
2. 选择 "Open an existing Android Studio project"
3. 导航到 `D:/workspace/workbuddy/house-monitor`
4. 点击 "Open"

### 步骤2：同步Gradle
1. Android Studio会自动检测并提示同步Gradle
2. 点击 "Sync Project with Gradle Files" 按钮
3. 等待依赖下载完成（首次可能需要几分钟）

### 步骤3：构建APK
1. 点击菜单栏 "Build" → "Build Bundle(s) / APK(s)"
2. 选择 "Build APK(s)"
3. 等待编译完成
4. APK将生成在 `app/build/outputs/apk/debug/` 目录下

## 方法二：使用命令行

### 前提条件
- 已安装Android Studio
- 已配置ANDROID_HOME环境变量
- 已安装Gradle（可选）

### 构建命令
```bash
# 进入项目目录
cd D:/workspace/workbuddy/house-monitor

# 使用Android Studio自带的Gradle Wrapper
# 在Windows上
gradlew.bat assembleDebug

# 在macOS/Linux上
./gradlew assembleDebug
```

### 输出位置
生成的APK文件将位于：
```
app/build/outputs/apk/debug/app-debug.apk
```

## 方法三：使用预编译脚本

由于网络环境问题，您也可以：

1. 将项目复制到有网络的电脑上
2. 使用Android Studio打开并编译
3. 将生成的APK文件复制回来

## 构建配置说明

### Debug版本
- 包名：com.housemonitor
- 签名：使用调试密钥
- 优化：关闭代码混淆
- 调试：支持调试功能

### Release版本（可选）
如需构建发布版本：
```bash
# Debug版本
./gradlew assembleDebug

# Release版本（需要配置签名）
./gradlew assembleRelease
```

## 常见问题解决

### 问题1：Gradle同步失败
**解决方案**：
- 检查网络连接
- 尝试使用代理
- 手动下载Gradle依赖

### 问题2：编译错误
**解决方案**：
- 清理项目：`./gradlew clean`
- 重新同步：`./gradlew --refresh-dependencies`
- 检查Android SDK版本

### 问题3：依赖下载慢
**解决方案**：
- 配置国内镜像源
- 使用阿里云Maven镜像
- 在 `build.gradle` 中添加：
```gradle
repositories {
    maven { url 'https://maven.aliyun.com/repository/google' }
    maven { url 'https://maven.aliyun.com/repository/jcenter' }
    maven { url 'https://maven.aliyun.com/repository/central' }
}
```

## 项目依赖说明

### 主要依赖
- Android Gradle Plugin: 8.2.0
- Kotlin: 1.9.10
- Jetpack Compose: 1.5.4
- Room: 2.6.1
- Hilt: 2.48
- WorkManager: 2.9.0

### SDK要求
- compileSdk: 34
- targetSdk: 33
- minSdk: 23

## 手动编译指南

如果无法使用Gradle，您可以：

1. **创建新的Android项目**
   - 在Android Studio中创建新项目
   - 选择 "Empty Activity" 模板
   - 配置相同的包名和SDK版本

2. **复制源代码**
   - 将本项目的 `java/com/housemonitor/` 目录复制到新项目
   - 复制 `res/` 目录下的资源文件
   - 复制 `AndroidManifest.xml`

3. **配置依赖**
   - 复制 `build.gradle` 中的依赖配置
   - 手动添加必要的依赖库

4. **编译运行**
   - 同步Gradle
   - 构建APK

## 快速部署脚本

### Windows PowerShell脚本
```powershell
# build.ps1
$projectPath = "D:\workspace\workbuddy\house-monitor"
Set-Location $projectPath

# 检查Android Studio是否安装
$androidStudioPath = "${env:ProgramFiles}\Android\Android Studio\bin\studio64.exe"
if (Test-Path $androidStudioPath) {
    Write-Host "Android Studio found. Opening project..."
    Start-Process $androidStudioPath -ArgumentList "$projectPath"
} else {
    Write-Host "Android Studio not found. Please install Android Studio first."
    Write-Host "Download from: https://developer.android.com/studio"
}
```

### macOS/Linux脚本
```bash
#!/bin/bash
# build.sh
PROJECT_PATH="D:/workspace/workbuddy/house-monitor"
cd "$PROJECT_PATH"

# 检查Android Studio
if command -v studio &> /dev/null; then
    echo "Opening project in Android Studio..."
    studio "$PROJECT_PATH"
else
    echo "Android Studio not found. Please install Android Studio first."
    echo "Download from: https://developer.android.com/studio"
fi
```

## 验证APK

构建完成后，可以使用以下方法验证APK：

### 1. 安装到设备
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. 检查APK信息
```bash
aapt dump badging app/build/outputs/apk/debug/app-debug.apk
```

### 3. 分析APK内容
```bash
# 解压APK查看内容
unzip -l app/build/outputs/apk/debug/app-debug.apk
```

## 性能优化建议

### 构建优化
- 启用Gradle守护进程
- 配置Gradle并行构建
- 使用构建缓存

### 在 `gradle.properties` 中添加：
```properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
android.enableBuildCache=true
```

## 联系支持

如果遇到构建问题，请检查：
1. Android Studio版本是否兼容
2. JDK版本是否正确（推荐JDK 17）
3. Android SDK是否完整安装
4. 网络连接是否正常

项目已完整实现所有功能，可以直接在Android Studio中打开使用。