# GitHub Actions 构建状态说明

本文件详细说明GitHub Actions的构建状态、配置和故障排除方法。

## 📊 构建状态徽章

在README.md中添加以下徽章来显示构建状态：

```markdown
[![Android CI/CD](https://github.com/forcoder/house-monitor/actions/workflows/android-build.yml/badge.svg)](https://github.com/forcoder/house-monitor/actions/workflows/android-build.yml)
[![Release](https://img.shields.io/github/v/release/你的用户名/house-monitor)](https://github.com/forcoder/house-monitor/releases)
[![License](https://img.shields.io/github/license/你的用户名/house-monitor)](https://github.com/forcoder/house-monitor/blob/main/LICENSE)
```

## 🔧 工作流配置详解

### android-build.yml 配置说明

```yaml
name: Android CI/CD

# 触发条件
on:
  push:
    branches: [ main, master ]  # 推送到主分支时触发
  pull_request:
    branches: [ main, master ]   # 创建PR时触发
  workflow_dispatch:             # 允许手动触发

jobs:
  build:
    runs-on: ubuntu-latest       # 使用最新的Ubuntu runner

    steps:
    # 步骤1: 检出代码
    - uses: actions/checkout@v4

    # 步骤2: 设置JDK 17
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'

    # 步骤3: 设置Android SDK
    - name: Setup Android SDK
      uses: android-actions/setup-android@v3
      with:
        cmdline-tools-version: '11076708'

    # 步骤4: 缓存Gradle依赖（加速构建）
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-

    # 步骤5: 授予gradlew执行权限
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    # 步骤6: 构建APK
    - name: Build with Gradle
      run: ./gradlew assembleDebug --no-daemon --stacktrace
      env:
        GRADLE_OPTS: -Xmx4g

    # 步骤7: 上传APK文件
    - name: Upload APK artifact
      uses: actions/upload-artifact@v3
      with:
        name: house-monitor-debug
        path: app/build/outputs/apk/debug/app-debug.apk
        if-no-files-found: error

    # 步骤8: 上传构建日志
    - name: Upload build logs
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: build-logs
        path: |
          app/build/reports/
          app/build/outputs/logs/
        if-no-files-found: ignore

  # Release工作流
  release:
    needs: build                  # 依赖build工作流
    if: github.ref == 'refs/heads/main' || github.ref == 'refs/heads/master'
    runs-on: ubuntu-latest

    steps:
    # 下载APK
    - name: Download APK artifact
      uses: actions/download-artifact@v3
      with:
        name: house-monitor-debug

    # 获取版本信息
    - name: Get version info
      id: version
      run: |
        VERSION_NAME=$(grep 'APP_VERSION_NAME' gradle.properties | cut -d'=' -f2)
        VERSION_CODE=$(grep 'APP_VERSION_CODE' gradle.properties | cut -d'=' -f2)
        echo "version_name=${VERSION_NAME}" >> $GITHUB_OUTPUT
        echo "version_code=${VERSION_CODE}" >> $GITHUB_OUTPUT

    # 创建GitHub Release
    - name: Create Release
      uses: softprops/action-gh-release@v1
      with:
        files: app-debug.apk
        tag_name: v${{ steps.version.outputs.version_name }}
        name: 房源监控 v${{ steps.version.outputs.version_name }}
        body: |
          ## 美团房源监控APP
          ### 版本信息
          - 版本号: v${{ steps.version.outputs.version_name }}
          - 版本码: ${{ steps.version.outputs.version_code }}
          - 构建时间: ${{ github.run_started_at }}
          ### 功能特点
          ✅ 自动监控美团小程序房源可用性
          ✅ 每5分钟检查一次房源状态
          ✅ 发现无房日期时发送系统通知
          ✅ 后台服务持续运行
          ✅ 设备重启后自动恢复监控
          ### 安装说明
          1. 下载APK文件
          2. 允许安装未知来源应用
          3. 安装并打开应用
          4. 输入美团小程序地址
          5. 点击"开始监控"
          ### 注意事项
          - 请允许应用发送通知
          - 美团小程序地址需要是有效的房源页面URL
          - 建议在电量充足的情况下使用
        draft: false
        prerelease: false
        generate_release_notes: true
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

## 🎯 构建触发条件

### 自动触发
1. **推送代码**: 推送到 `main` 或 `master` 分支
2. **Pull Request**: 创建或更新PR
3. **定时构建**: 可以添加定时触发器

### 手动触发
1. 进入GitHub仓库的 "Actions" 页面
2. 选择 "Android CI/CD" 工作流
3. 点击 "Run workflow"
4. 选择分支并运行

## 📈 构建性能指标

### 预期构建时间
- **首次构建**: 10-15分钟（下载依赖）
- **缓存构建**: 3-5分钟
- **增量构建**: 1-2分钟

### 资源使用
- **内存**: 最大4GB
- **存储空间**: 约2-3GB
- **网络带宽**: 依赖下载需要较好的网络连接

## 🚨 常见构建问题

### 问题1: Gradle依赖下载失败
**症状**: 构建在下载依赖时失败
**原因**: 网络连接问题或仓库访问限制
**解决方案**:
```yaml
# 在gradle.properties中添加镜像
systemProp.http.proxyHost=mirrors.aliyun.com
systemProp.http.proxyPort=80
```

### 问题2: Android SDK组件缺失
**症状**: 构建失败，提示SDK组件缺失
**解决方案**:
```yaml
# 更新Android SDK版本
with:
  cmdline-tools-version: '11076708'
```

### 问题3: 内存不足
**症状**: 构建过程中出现OutOfMemoryError
**解决方案**:
```yaml
env:
  GRADLE_OPTS: -Xmx8g -Xms2g
```

### 问题4: 版本冲突
**症状**: 依赖版本不兼容导致构建失败
**解决方案**:
```kotlin
// 检查build.gradle.kts中的版本兼容性
android {
    compileSdk = 34
    targetSdk = 34
}
```

## 🔍 构建日志分析

### 关键日志信息
```bash
# 成功构建
BUILD SUCCESSFUL in 5m 23s

# 构建失败
BUILD FAILED in 2m 15s

# 依赖下载
Downloading https://services.gradle.org/distributions/gradle-8.4-bin.zip

# Android SDK安装
Installing Android SDK Platform 34
```

### 日志查看方法
1. 进入Actions页面
2. 点击具体的构建运行
3. 查看 "Build with Gradle" 步骤的日志
4. 下载 "build-logs" artifact查看详细日志

## 🛠 高级配置选项

### 并行构建
```yaml
strategy:
  matrix:
    api-level: [24, 28, 30, 34]
    target: [default, google_apis]
```

### 测试集成
```yaml
- name: Run Unit Tests
  run: ./gradlew test --no-daemon

- name: Run Instrumentation Tests
  run: ./gradlew connectedAndroidTest --no-daemon
```

### 代码质量检查
```yaml
- name: Run Lint
  run: ./gradlew lint --no-daemon

- name: Run Code Coverage
  run: ./gradlew jacocoTestReport --no-daemon
```

### 安全扫描
```yaml
- name: Security Scan
  uses: actions/codeql-action/analyze@v2
  with:
    language: java-kotlin
```

## 📊 构建监控

### 成功率监控
- 跟踪构建成功率
- 记录失败原因
- 设置失败告警

### 性能监控
- 构建时间趋势
- 资源使用情况
- 依赖下载速度

### 质量监控
- 测试覆盖率
- 代码质量评分
- 安全漏洞检测

## 🔄 构建优化建议

### 1. 依赖缓存优化
```yaml
# 缓存更多目录
path: |
  ~/.gradle/caches
  ~/.gradle/wrapper
  ~/.android/build-cache
  app/build/intermediates
```

### 2. 增量构建
```yaml
# 只构建变更的部分
run: ./gradlew assembleDebug --no-daemon --build-cache
```

### 3. 并行执行
```yaml
# 并行执行测试
run: ./gradlew test --parallel --no-daemon
```

### 4. 构建矩阵
```yaml
# 多环境测试
strategy:
  matrix:
    os: [ubuntu-latest, windows-latest, macos-latest]
    java: [8, 11, 17]
```

## 📞 获取帮助

### 官方文档
- [GitHub Actions文档](https://docs.github.com/actions)
- [Android CI/CD指南](https://developer.android.com/studio/build/github-actions)

### 社区支持
- 在GitHub Issues中提问
- 参与Stack Overflow讨论
- 加入Android开发者社区

### 调试工具
- 使用Actions的调试功能
- 查看详细的构建日志
- 使用本地环境复现问题