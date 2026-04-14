# APK构建说明

由于本地环境限制，无法直接构建APK文件。以下是构建APK的详细步骤：

## 环境要求

### 必需软件
1. **Android Studio** (推荐Giraffe版本或更高)
2. **JDK 8** 或 **JDK 11**
3. **Android SDK** (API Level 34)
4. **Gradle** (7.5版本)

### 硬件要求
- 至少4GB RAM (推荐8GB以上)
- 至少2GB可用磁盘空间
- Windows 10/11, macOS, 或 Linux

## 构建步骤

### 方法一：使用Android Studio (推荐)

1. **安装Android Studio**
   - 下载并安装 [Android Studio Giraffe](https://developer.android.com/studio)
   - 安装过程中选择安装Android SDK和Android Virtual Device

2. **打开项目**
   ```bash
   # 使用Android Studio打开项目
   File -> Open -> 选择项目目录 D:\workspace\workbuddy\house
   ```

3. **同步Gradle**
   - Android Studio会自动检测并同步Gradle依赖
   - 等待Gradle同步完成（可能需要几分钟）

4. **配置签名证书（可选，用于发布版本）**
   - 创建keystore文件或使用现有证书
   - 在 `app/build.gradle` 中配置签名信息

5. **构建APK**
   ```bash
   # Debug版本（快速构建，用于测试）
   Build -> Build Bundle(s) / APK(s) -> Build APK(s)
   
   # Release版本（正式版本，需要签名）
   Build -> Generate Signed Bundle or APK -> APK
   ```

### 方法二：使用命令行

1. **配置环境变量**
   ```bash
   # 设置Android SDK路径
   export ANDROID_HOME=/path/to/android/sdk
   export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools
   ```

2. **构建Debug版本**
   ```bash
   cd D:\workspace\workbuddy\house
   ./gradlew assembleDebug
   ```

3. **构建Release版本**
   ```bash
   ./gradlew assembleRelease
   ```

4. **查找生成的APK**
   ```bash
   # Debug APK位置
   app/build/outputs/apk/debug/app-debug.apk
   
   # Release APK位置
   app/build/outputs/apk/release/app-release.apk
   ```

## 项目配置说明

### 修改应用信息
编辑 `app/build.gradle` 文件：

```gradle
android {
    defaultConfig {
        applicationId "com.workbuddy.house.monitor"  // 包名
        versionCode 1                                 // 版本号
        versionName "1.0"                            // 版本名称
    }
}
```

### 配置签名证书
创建 `keystore.properties` 文件：

```properties
storePassword=your_store_password
keyPassword=your_key_password
keyAlias=your_key_alias
storeFile=../keystore.jks
```

在 `app/build.gradle` 中添加：

```gradle
def keystoreProperties = new Properties()
keystoreProperties.load(new FileInputStream(rootProject.file("keystore.properties")))

android {
    signingConfigs {
        release {
            keyAlias keystoreProperties['keyAlias']
            keyPassword keystoreProperties['keyPassword']
            storeFile file(keystoreProperties['storeFile'])
            storePassword keystoreProperties['storePassword']
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

## 常见问题解决

### 1. Gradle同步失败
**问题**: 依赖下载失败或版本不兼容
**解决**: 
- 检查网络连接
- 更新Gradle版本
- 清除Gradle缓存：`rm -rf ~/.gradle/caches/`

### 2. SDK版本问题
**问题**: `compileSdk` 或 `targetSdk` 版本不匹配
**解决**:
- 在Android Studio中安装对应的SDK版本
- 更新 `app/build.gradle` 中的SDK版本

### 3. 内存不足
**问题**: 构建过程中内存溢出
**解决**:
- 增加Gradle内存：编辑 `gradle.properties`
  ```properties
  org.gradle.jvmargs=-Xmx4096m
  ```
- 关闭其他占用内存的程序

### 4. 权限问题
**问题**: 文件访问权限不足
**解决**:
- 确保项目目录有读写权限
- 以管理员身份运行命令行

## 测试APK

### 安装到设备
```bash
# 连接Android设备并启用USB调试
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 使用模拟器
1. 在Android Studio中创建Android Virtual Device (AVD)
2. 启动模拟器
3. 安装APK到模拟器

## 发布准备

### 版本号管理
- `versionCode`: 整数，每次发布递增
- `versionName`: 字符串，用户可见的版本号

### 应用商店要求
- Google Play要求APK大小不超过100MB
- 需要提供应用图标、截图、描述等
- 需要隐私政策链接

## 构建验证

成功构建后，检查以下内容：

1. **APK完整性**
   ```bash
   # 检查APK是否有效
   aapt dump badging app-debug.apk
   ```

2. **权限配置**
   - 确认 `AndroidManifest.xml` 中的权限配置正确
   - 检查运行时权限请求逻辑

3. **功能测试**
   - 测试美团URL输入功能
   - 验证监控启动/停止功能
   - 检查通知推送功能
   - 测试开机自启动功能

## 性能优化建议

### 构建优化
1. **启用Gradle守护进程**
   ```properties
   # gradle.properties
   org.gradle.daemon=true
   ```

2. **并行构建**
   ```properties
   org.gradle.parallel=true
   ```

3. **配置构建缓存**
   ```properties
   org.gradle.caching=true
   ```

### 应用优化
1. **ProGuard混淆** (Release版本)
2. **资源压缩**
3. **代码优化**

## 后续维护

### 版本更新
- 更新依赖库版本
- 适配新的Android版本
- 修复已知问题

### 监控和维护
- 监控应用崩溃日志
- 收集用户反馈
- 定期更新API接口

如需进一步帮助，请参考 [Android开发者文档](https://developer.android.com/guide) 或联系开发团队。