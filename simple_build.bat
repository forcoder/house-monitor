@echo off
chcp 65001 >nul
echo ========================================
echo 美团房源监控 - 简化构建脚本
echo ========================================
echo.

:: 设置环境变量
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot
set ANDROID_HOME=D:\Android\SDK
set ANDROID_SDK_ROOT=D:\Android\SDK
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\tools;%PATH%

cd /d D:\workspace\workbuddy\house

echo 当前目录: %cd%
echo.

:: 检查必要的环境
echo [0/2] 检查环境...

where java >nul 2>nul
if errorlevel 1 (
    echo [错误] Java未安装或未配置
    pause
    exit /b 1
) else (
    java -version
    echo ✓ Java环境正常
)

echo.

:: 检查Android SDK
if exist "%ANDROID_HOME%" (
    echo ✓ Android SDK路径存在: %ANDROID_HOME%
) else (
    echo [警告] Android SDK路径不存在: %ANDROID_HOME%
    echo [警告] 请手动安装Android SDK
    pause
    exit /b 1
)

echo.

:: 显示项目信息
echo [1/2] 项目信息...
echo 应用名称: 房源监控
type gradle.properties | findstr "APP_VERSION"
echo.

:: 检查项目文件
if exist "app\build.gradle.kts" (
    echo ✓ 项目配置文件存在
) else (
    echo [错误] 项目配置文件不存在
    pause
    exit /b 1
)

echo.

echo [2/2] 构建说明...
echo.
echo 由于Gradle wrapper文件存在问题，无法直接构建APK。
echo 请使用以下方法之一进行构建:
echo.
echo 方法一: 使用Android Studio
echo 1. 打开Android Studio
echo 2. 选择 "Open an existing Android Studio project"
echo 3. 选择目录: D:\workspace\workbuddy\house
echo 4. 等待Gradle同步完成
echo 5. 点击 "Build" -> "Build Bundle(s) / APK(s)" -> "Build APK(s)"
echo.
echo 方法二: 安装Gradle后手动构建
echo 1. 下载并安装Gradle: https://gradle.org/releases/
echo 2. 配置环境变量
echo 3. 运行命令: gradle assembleDebug
echo.
echo 方法三: 使用在线构建服务
echo 1. 将项目上传到GitHub
echo 2. 使用GitHub Actions或其他CI服务进行构建

echo.
echo 项目已准备就绪，可以使用以上任意方法进行构建。
echo.
echo ========================================
echo 构建说明完成
echo ========================================
echo.
pause