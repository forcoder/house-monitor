# GitHub在线构建设置指南

本指南将帮助你将房源监控项目上传到GitHub并使用GitHub Actions进行自动化构建。

## 准备工作

### 1. GitHub账号
- 如果没有GitHub账号，请访问 [github.com](https://github.com) 注册
- 确保你有推送代码到GitHub的权限

### 2. Git工具
- 安装 [Git for Windows](https://git-scm.com/download/win)
- 配置Git用户信息：
  ```bash
  git config --global user.name "你的用户名"
  git config --global user.email "你的邮箱"
  ```

## 步骤一：创建GitHub仓库

1. 登录GitHub网站
2. 点击右上角的 "+" 按钮，选择 "New repository"
3. 填写仓库信息：
   - Repository name: `house-monitor`
   - Description: `美团房源监控APP - 自动监控房源可用性并发送通知`
   - 选择 Public 或 Private
   - 不要勾选 "Initialize this repository with a README"
4. 点击 "Create repository"

## 步骤二：上传项目代码

### 方法A：使用命令行（推荐）

1. 打开命令提示符或Git Bash
2. 导航到项目目录：
   ```bash
   cd D:\workspace\workbuddy\house
   ```

3. 初始化Git仓库：
   ```bash
   git init
   ```

4. 添加远程仓库（替换为你的仓库URL）：
   ```bash
   git remote add origin https://github.com/forcoder/house-monitor.git
   ```

5. 添加所有文件并提交：
   ```bash
   git add .
   git commit -m "初始化项目：美团房源监控APP"
   ```

6. 推送到GitHub：
   ```bash
   git push -u origin main
   ```

### 方法B：使用GitHub Desktop

1. 下载并安装 [GitHub Desktop](https://desktop.github.com/)
2. 打开GitHub Desktop并登录
3. 选择 "Add an existing repository"
4. 选择项目目录 `D:\workspace\workbuddy\house`
5. 点击 "Publish repository"
6. 填写仓库信息并发布

## 步骤三：配置GitHub Actions

### 1. 检查工作流文件
确保项目根目录有以下文件：
- `.github/workflows/android-build.yml`

### 2. 配置Android SDK许可
在GitHub仓库设置中，可能需要配置Android SDK许可：

1. 进入仓库的 "Settings" 页面
2. 选择 "Secrets and variables" → "Actions"
3. 添加以下Secrets（如果需要）：
   - `ANDROID_KEYSTORE`: 签名密钥库（用于Release版本）
   - `ANDROID_KEYSTORE_PASSWORD`: 密钥库密码
   - `ANDROID_KEY_ALIAS`: 密钥别名
   - `ANDROID_KEY_PASSWORD`: 密钥密码

## 步骤四：触发构建

### 自动触发
- 推送代码到 `main` 或 `master` 分支会自动触发构建
- 创建Pull Request也会触发构建

### 手动触发
1. 进入仓库的 "Actions" 页面
2. 选择 "Android CI/CD" 工作流
3. 点击 "Run workflow"
4. 选择分支并运行

## 步骤五：下载构建结果

### 从Actions下载
1. 进入 "Actions" 页面
2. 点击最新的工作流运行
3. 在 "Artifacts" 部分下载：
   - `house-monitor-debug`: APK文件
   - `build-logs`: 构建日志（用于调试）

### 从Release下载
1. 进入仓库的 "Releases" 页面
2. 选择最新版本
3. 下载APK文件

## 故障排除

### 1. 构建失败
**问题**: Gradle依赖下载失败
**解决**: 
- 检查网络连接
- 查看构建日志中的具体错误
- 尝试重新运行工作流

### 2. Android SDK问题
**问题**: Android SDK组件缺失
**解决**:
- 检查工作流日志
- 更新Android SDK版本配置
- 在GitHub Issues中报告问题

### 3. 内存不足
**问题**: 构建过程中内存溢出
**解决**:
- 增加Gradle内存配置
- 优化依赖项
- 使用更强大的runner

### 4. 版本冲突
**问题**: 依赖版本不兼容
**解决**:
- 检查 `build.gradle.kts` 中的版本号
- 更新到兼容的版本
- 查看Android官方文档

## 高级配置

### 自定义构建参数
编辑 `.github/workflows/android-build.yml`：

```yaml
# 修改Java版本
with:
  java-version: '17'  # 或 '11', '8'

# 修改Android SDK版本
with:
  cmdline-tools-version: '11076708'

# 修改Gradle参数
run: ./gradlew assembleDebug --no-daemon --stacktrace --info
env:
  GRADLE_OPTS: -Xmx8g  # 增加内存
```

### 添加测试
在工作流中添加测试步骤：

```yaml
- name: Run Unit Tests
  run: ./gradlew test --no-daemon

- name: Run Instrumentation Tests
  run: ./gradlew connectedAndroidTest --no-daemon
```

### 构建Release版本
添加Release构建工作流：

```yaml
- name: Build Release APK
  run: ./gradlew assembleRelease --no-daemon
  env:
    ANDROID_KEYSTORE: ${{ secrets.ANDROID_KEYSTORE }}
    ANDROID_KEYSTORE_PASSWORD: ${{ secrets.ANDROID_KEYSTORE_PASSWORD }}
```

## 安全建议

### 1. 敏感信息保护
- 不要在代码中硬编码API密钥
- 使用环境变量或Secrets
- 定期轮换密钥

### 2. 代码审查
- 启用分支保护规则
- 要求Pull Request审查
- 启用状态检查

### 3. 依赖安全
- 定期更新依赖版本
- 使用依赖扫描工具
- 检查安全漏洞

## 持续集成最佳实践

### 1. 提交规范
- 使用清晰的提交信息
- 遵循约定式提交规范
- 定期同步主分支

### 2. 分支策略
- `main`: 主分支，稳定版本
- `develop`: 开发分支
- `feature/*`: 功能分支
- `hotfix/*`: 紧急修复分支

### 3. 版本管理
- 使用语义化版本号
- 自动版本递增
- 发布说明模板

## 项目维护

### 定期任务
1. 更新依赖版本
2. 检查安全漏洞
3. 清理过时的Actions
4. 优化构建性能
5. 更新文档

### 监控和通知
- 启用构建状态通知
- 监控构建成功率
- 设置构建超时告警

## 参考资源

- [GitHub Actions文档](https://docs.github.com/actions)
- [Android CI/CD最佳实践](https://developer.android.com/studio/build/github-actions)
- [Gradle用户指南](https://docs.gradle.org/current/userguide/userguide.html)

如需进一步帮助，请参考以上文档或联系开发团队。