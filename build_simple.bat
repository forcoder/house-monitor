@echo off
REM 美团房源监控APP - 简化构建脚本
REM 这个脚本帮助用户在Windows上构建APK

echo ============================================
echo    美团房源监控APP - APK构建工具
echo ============================================
echo.

REM 检查Java环境
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到Java环境
    echo 请安装JDK 8或更高版本
    echo 下载地址: https://adoptium.net/
    pause
    exit /b 1
)

echo [✓] Java环境检测通过

REM 检查Android Studio
if exist "%ProgramFiles%\Android\Android Studio\bin\studio64.exe" (
    echo [✓] 检测到Android Studio
    set "ANDROID_STUDIO_FOUND=true"
) else (
    echo [警告] 未检测到Android Studio
    set "ANDROID_STUDIO_FOUND=false"
)

REM 显示项目信息
echo.
echo 项目信息:
echo ------------------------
echo 项目名称: 美团房源监控APP
echo 包名: com.housemonitor
echo 最低SDK: Android 6.0 (API 23)
echo 目标SDK: Android 13 (API 33)
echo 编译SDK: Android 14 (API 34)
echo.

REM 检查项目文件
if not exist "app\build.gradle" (
    echo [错误] 未找到项目配置文件
    echo 请确保在正确的项目目录下运行此脚本
    pause
    exit /b 1
)

echo [✓] 项目文件检查通过

REM 提供构建选项
echo.
echo 构建选项:
echo ------------------------
echo 1. 使用Android Studio打开项目 (推荐)
echo 2. 查看构建说明
echo 3. 退出
echo.

set /p choice="请选择操作 (1-3): "

if "%choice%"=="1" (
    echo.
    echo 正在启动Android Studio...
    if "%ANDROID_STUDIO_FOUND%"=="true" (
        start "" "%ProgramFiles%\Android\Android Studio\bin\studio64.exe" "%CD%"
        echo Android Studio已启动，请按照以下步骤操作:
        echo 1. 等待Gradle同步完成
        echo 2. 点击 Build -> Build Bundle(s) / APK(s) -> Build APK(s)
        echo 3. APK将生成在 app\build\outputs\apk\debug\ 目录下
    ) else (
        echo 请手动启动Android Studio并打开此项目目录
        echo 项目路径: %CD%
    )
) else if "%choice%"=="2" (
    echo.
    echo 构建说明:
    echo =====================================
    echo.
    echo 前提条件:
    echo - Android Studio (推荐Hedgehog版本或更高)
    echo - JDK 17
    echo - 稳定的网络连接 (用于下载依赖)
    echo.
    echo 构建步骤:
    echo 1. 使用Android Studio打开项目
    echo 2. 等待Gradle同步完成 (首次可能需5-10分钟)
    echo 3. 点击菜单: Build -> Build APK(s)
    echo 4. 等待编译完成
    echo 5. 在 app\build\outputs\apk\debug\ 找到APK文件
    echo.
    echo 输出文件:
    echo - app-debug.apk (调试版本)
    echo - 文件大小: 约15-25MB
    echo - 支持Android 6.0+
    echo.
    echo 常见问题:
    echo - Gradle同步失败: 检查网络连接
    echo - 编译错误: 清理项目后重试
    echo - 依赖下载慢: 配置国内镜像源
    echo.
    echo 更多帮助请查看 README.md 文件
) else (
    echo 退出程序
    exit /b 0
)

echo.
echo 按任意键退出...
pause >nul