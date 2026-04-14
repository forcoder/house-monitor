@echo off
chcp 65001 >nul
echo ========================================
echo 美团房源监控 - 编译构建
echo ========================================
echo.

:: 设置环境变量
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot
set ANDROID_HOME=D:\Android\SDK
set ANDROID_SDK_ROOT=D:\Android\SDK
set PATH=%JAVA_HOME%\bin;%PATH%
set GRADLE_OPTS=-Xmx4096m

cd /d D:\workspace\workbuddy\house

echo 当前目录: %cd%
echo.

:: 检查必要的文件
if not exist "gradlew.bat" (
    echo [错误] gradlew.bat 不存在
    pause
    exit /b 1
)

if not exist "app\build.gradle.kts" (
    echo [错误] app\build.gradle.kts 不存在
    pause
    exit /b 1
)

echo [0/3] 检查环境...

:: 检查Java
where java >nul 2>nul
if errorlevel 1 (
    echo [错误] Java未安装或未配置
    pause
    exit /b 1
) else (
    echo ✓ Java已安装
)

:: 检查Android SDK
if not exist "%ANDROID_HOME%" (
    echo [警告] Android SDK路径不存在: %ANDROID_HOME%
    echo [警告] 请手动配置Android SDK路径
) else (
    echo ✓ Android SDK已配置
)

echo.

:: 1. 清理项目
echo [1/3] 清理项目...
call gradlew.bat clean --no-daemon
if errorlevel 1 (
    echo.
    echo [警告] 清理失败，继续构建...
) else (
    echo ✓ 清理完成
)

echo.

:: 2. 编译 APK（Gradle 会自动执行 incrementVersion 递增版本号）
echo [2/3] 编译 APK（自动递增版本号）...
call gradlew.bat assembleDebug --no-daemon --stacktrace
if errorlevel 1 (
    echo.
    echo [错误] 编译失败
    echo 请检查以下可能的问题:
    echo 1. Android SDK未正确安装
    echo 2. 网络连接问题导致依赖下载失败
    echo 3. 代码编译错误
    echo 4. 内存不足
    pause
    exit /b 1
)

echo.
echo ✓ 编译完成

echo.

:: 3. 显示构建结果
echo [3/3] 构建结果...
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo ✓ APK构建成功!
    echo.
    echo APK位置: app\build\outputs\apk\debug\app-debug.apk

    :: 获取APK信息
    for /f "tokens=*" %%i in ('powershell -Command "(Get-Item 'app\build\outputs\apk\debug\app-debug.apk').Length"') do set "apk_size=%%i"
    set /a apk_size_mb=%apk_size%/1024/1024
    echo APK大小: %apk_size_mb% MB

    :: 显示版本信息
    if exist "gradle.properties" (
        for /f "tokens=2 delims==" %%a in ('findstr "APP_VERSION_NAME" gradle.properties') do set "version_name=%%a"
        for /f "tokens=2 delims==" %%a in ('findstr "APP_VERSION_CODE" gradle.properties') do set "version_code=%%a"
        echo 版本信息: v%version_name% (%version_code%)
    )

    echo.
    echo 安装命令: adb install app\build\outputs\apk\debug\app-debug.apk

) else (
    echo [错误] APK文件未生成
    echo 请检查构建日志获取更多信息
    pause
    exit /b 1
)

echo.
echo ========================================
echo 构建完成！
echo ========================================
echo.

:: 询问是否安装到设备
echo 是否要安装到连接的设备? (y/n)
set /p install=
if /i "%install%"=="y" (
    echo 正在安装...
    adb install app\build\outputs\apk\debug\app-debug.apk
    if errorlevel 1 (
        echo [错误] 安装失败，请确保设备已连接并启用USB调试
    ) else (
        echo ✓ 安装成功!
    )
)

echo.
pause