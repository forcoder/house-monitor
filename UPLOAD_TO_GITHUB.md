# 🚀 上传项目到GitHub完整指南

本指南将详细教你如何将房源监控项目上传到GitHub并使用在线构建服务。

## 📋 准备工作

### 1. 创建GitHub账号
如果还没有GitHub账号：
1. 访问 [github.com](https://github.com)
2. 点击 "Sign up"
3. 填写用户名、邮箱和密码
4. 完成邮箱验证

### 2. 安装Git
1. 下载 [Git for Windows](https://git-scm.com/download/win)
2. 运行安装程序（使用默认选项）
3. 验证安装：
   ```bash
   git --version
   ```

### 3. 配置Git
```bash
# 设置用户名和邮箱
git config --global user.name "你的GitHub用户名"
git config --global user.email "你的GitHub邮箱"

# 验证配置
git config --list
```

## 🔧 步骤一：创建GitHub仓库

### 1. 登录GitHub
打开浏览器，登录你的GitHub账号

### 2. 创建新仓库
1. 点击右上角的 "+" 按钮
2. 选择 "New repository"

### 3. 填写仓库信息
- **Repository name**: `house-monitor`
- **Description**: `美团房源监控APP - 自动监控房源可用性并发送通知`
- **Visibility**: 选择 Public（公开）或 Private（私有）
- **Initialize this repository with a README**: ❌ 不勾选
- **Add .gitignore**: ❌ 不选择
- **Choose a license**: ❌ 不选择

### 4. 创建仓库
点击 "Create repository" 按钮

## 📤 步骤二：上传项目代码

### 方法A：使用命令行（推荐）

#### 1. 打开命令提示符
按 `Win + R`，输入 `cmd`，回车

#### 2. 导航到项目目录
```bash
cd /d D:\workspace\workbuddy\house
```

#### 3. 初始化Git仓库
```bash
git init
```

#### 4. 添加远程仓库地址
将下面的URL替换为你创建的仓库地址：
```bash
git remote add origin https://github.com/forcoder/house-monitor.git
```

#### 5. 添加所有文件到Git
```bash
git add .
```

#### 6. 提交代码
```bash
git commit -m "🎉 初始化项目：美团房源监控APP"
```

#### 7. 推送到GitHub
```bash
git push -u origin main
```

> 如果出现 `main` 分支不存在的问题，尝试：
> ```bash
> git branch -M main
> git push -u origin main
> ```

### 方法B：使用GitHub Desktop

#### 1. 下载并安装GitHub Desktop
访问 [desktop.github.com](https://desktop.github.com/) 下载安装

#### 2. 登录GitHub Desktop
1. 打开GitHub Desktop
2. 点击 "Sign in to GitHub.com"
3. 使用你的GitHub账号登录

#### 3. 添加本地仓库
1. 点击 "File" → "Add local repository"
2. 选择项目目录：`D:\workspace\workbuddy\house`
3. 点击 "Add repository"

#### 4. 发布到GitHub
1. 点击 "Publish repository" 按钮
2. 填写仓库信息：
   - Name: `house-monitor`
   - Description: `美团房源监控APP - 自动监控房源可用性并发送通知`
   - Keep this code private: 根据需求选择
3. 点击 "Publish repository"

## ✅ 步骤三：验证上传结果

### 1. 检查GitHub仓库
1. 打开你的GitHub仓库页面
2. 确认所有文件都已上传
3. 检查文件结构是否正确

### 2. 验证关键文件
确保以下文件存在：
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `.github/workflows/android-build.yml`
- `gradle.properties`
- `README.md`

### 3. 检查Actions配置
1. 进入仓库的 "Actions" 标签页
2. 确认 "Android CI/CD" 工作流已存在
3. 工作流应该显示为就绪状态

## 🚀 步骤四：触发首次构建

### 方法一：自动触发
推送代码后，Actions会自动触发构建

### 方法二：手动触发
1. 进入仓库的 "Actions" 页面
2. 选择 "Android CI/CD" 工作流
3. 点击 "Run workflow"
4. 选择 `main` 分支
5. 点击 "Run workflow"

## 📥 步骤五：下载构建的APK

### 1. 等待构建完成
- 构建通常需要5-15分钟
- 可以在Actions页面查看进度

### 2. 从Actions下载
1. 进入Actions页面
2. 点击最新的成功构建
3. 在 "Artifacts" 部分找到 `house-monitor-debug`
4. 点击下载APK文件

### 3. 从Release下载（推荐）
1. 构建完成后，会自动创建Release
2. 进入仓库的 "Releases" 页面
3. 下载最新版本的APK文件

## 🔧 故障排除

### 问题1：Git推送失败
**错误信息**: `remote: Permission to user/repo.git denied`
**解决方案**:
1. 检查仓库URL是否正确
2. 确认你有推送权限
3. 尝试使用SSH密钥：
   ```bash
   git remote set-url origin git@github.com:用户名/house-monitor.git
   ```

### 问题2：文件过大被拒绝
**错误信息**: `File too large`
**解决方案**:
1. 检查是否有大文件被意外添加
2. 创建 `.gitignore` 文件排除大文件
3. 使用 `git rm --cached` 移除大文件

### 问题3：Actions构建失败
**错误信息**: 构建状态显示失败
**解决方案**:
1. 点击失败的构建查看详情
2. 检查构建日志中的错误信息
3. 根据错误信息修复问题
4. 重新推送代码触发构建

### 问题4：网络连接问题
**错误信息**: 连接超时或网络错误
**解决方案**:
1. 检查网络连接
2. 尝试使用VPN
3. 分段推送大文件

## 📱 步骤六：安装和使用APK

### 1. 安装APK
1. 将下载的APK文件传输到安卓设备
2. 在设备上允许安装未知来源应用
3. 点击APK文件开始安装

### 2. 首次使用
1. 打开应用
2. 授予必要的权限（通知权限）
3. 输入美团小程序地址
4. 点击"开始监控"

### 3. 验证功能
1. 确认监控状态显示正常
2. 检查通知权限是否已开启
3. 测试监控功能是否正常工作

## 🔄 后续更新

### 更新代码
```bash
# 1. 修改代码
# 2. 添加更改
cd /d D:\workspace\workbuddy\house
git add .

# 3. 提交更改
git commit -m "描述你的更改"

# 4. 推送到GitHub
git push origin main
```

### 查看构建状态
1. 进入GitHub仓库的Actions页面
2. 查看最新的构建状态
3. 下载新版本的APK

## 🎯 项目维护建议

### 1. 定期更新
- 定期检查依赖更新
- 更新到最新的安全版本
- 保持代码质量

### 2. 版本管理
- 使用有意义的版本号
- 编写清晰的发布说明
- 保持版本历史记录

### 3. 文档维护
- 更新README文件
- 添加使用截图
- 完善API文档

### 4. 社区互动
- 及时回复Issues
- 处理Pull Requests
- 参与功能讨论

## 📞 获取帮助

### GitHub官方支持
- [GitHub帮助文档](https://docs.github.com/)
- [GitHub社区论坛](https://github.community/)

### 项目特定问题
- 在仓库的Issues中提问
- 查看已有的Issues和解决方案
- 联系项目维护者

## 🎉 恭喜！

你已成功将房源监控项目上传到GitHub并设置了自动化构建！现在你可以：

✅ 随时从GitHub下载最新版本的APK  
✅ 享受自动化构建带来的便利  
✅ 与社区分享你的项目  
✅ 持续改进和维护应用  

**🌟 别忘了给项目一个Star，支持开源发展！**