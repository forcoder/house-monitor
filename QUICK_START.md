# 🚀 快速开始：提交代码到GitHub并开始构建

由于网络连接问题，我将为你提供详细的手动操作步骤。

## 📋 准备工作

### 1. 确保你有GitHub账号
- 用户名：`forcoder`
- 访问：https://github.com/forcoder

### 2. 安装必要的工具
- Git: https://git-scm.com/download/win
- 可选：GitHub Desktop: https://desktop.github.com/

## 🔧 步骤一：创建GitHub仓库

### 方法A：网页创建（推荐）

1. **登录GitHub**
   - 打开 https://github.com
   - 使用你的账号登录

2. **创建新仓库**
   - 点击右上角的 "+" 按钮
   - 选择 "New repository"

3. **填写仓库信息**
   - **Repository name**: `house-monitor`
   - **Description**: `美团房源监控APP - 自动监控房源可用性并发送通知`
   - **Visibility**: Public（公开）
   - **Initialize this repository with a README**: ❌ 不勾选
   - **Add .gitignore**: ❌ 不选择
   - **Add a license**: ❌ 不选择

4. **创建仓库**
   - 点击 "Create repository" 按钮

### 方法B：使用GitHub CLI

```bash
# 安装GitHub CLI: https://cli.github.com/
gh repo create house-monitor --public --description "美团房源监控APP"
```

## 📤 步骤二：上传代码到GitHub

### 方法A：使用命令行

#### 1. 打开命令提示符
按 `Win + R`，输入 `cmd`，回车

#### 2. 导航到项目目录
```bash
cd /d D:\workspace\workbuddy\house
```

#### 3. 初始化Git仓库（如果还没有）
```bash
git init
git add .
git commit -m "🎉 初始化项目：美团房源监控APP"
```

#### 4. 添加远程仓库
```bash
git remote add origin https://github.com/forcoder/house-monitor.git
```

#### 5. 推送到GitHub
```bash
git push -u origin main
```

> 如果提示分支不存在，先创建main分支：
> ```bash
> git branch -M main
> git push -u origin main
> ```

### 方法B：使用GitHub Desktop

1. **打开GitHub Desktop**
2. **添加本地仓库**
   - 点击 "File" → "Add local repository"
   - 选择目录：`D:\workspace\workbuddy\house`
3. **发布到GitHub**
   - 点击 "Publish repository"
   - Repository name: `house-monitor`
   - 选择 Public
   - 点击 "Publish repository"

### 方法C：使用GitHub网页上传

1. **进入你的新仓库页面**
   - https://github.com/forcoder/house-monitor

2. **点击 "Add file" → "Upload files"**

3. **拖拽或选择所有项目文件**
   - 确保包含所有子文件夹
   - 特别是 `.github/workflows/android-build.yml`

4. **提交更改**
   - 填写提交信息："🎉 初始化项目：美团房源监控APP"
   - 点击 "Commit changes"

## ✅ 步骤三：验证上传结果

### 1. 检查仓库内容
访问：https://github.com/forcoder/house-monitor

确保包含以下重要文件：
- ✅ `app/build.gradle.kts`
- ✅ `build.gradle.kts`
- ✅ `settings.gradle.kts`
- ✅ `.github/workflows/android-build.yml`
- ✅ `gradle.properties`
- ✅ `README.md`
- ✅ 所有源代码文件

### 2. 检查Actions配置
1. 点击仓库顶部的 "Actions" 标签页
2. 确认看到 "Android CI/CD" 工作流
3. 如果提示启用Actions，点击 "Enable"

## 🚀 步骤四：触发构建

### 方法一：手动触发（推荐）

1. **进入Actions页面**
   - 点击仓库顶部的 "Actions" 标签页

2. **选择工作流**
   - 选择 "Android CI/CD" 工作流

3. **运行工作流**
   - 点击右上角的 "Run workflow"
   - 选择分支：`main`
   - 点击 "Run workflow"

4. **等待构建**
   - 构建通常需要5-15分钟
   - 可以在页面查看实时进度

### 方法二：推送代码触发

```bash
# 进行任何修改（可选）
git add .
git commit -m "更新代码"
git push origin main
```

推送后会自动触发构建。

## 📥 步骤五：下载APK

### 构建完成后，通过以下方式下载：

**方式一：从Releases下载（推荐）**
1. 进入仓库的 "Releases" 页面
2. 找到最新版本的Release
3. 下载 `app-debug.apk` 文件

**方式二：从Actions下载**
1. 进入 "Actions" 页面
2. 点击最新的成功构建
3. 在 "Artifacts" 部分下载 `house-monitor-debug`

## 📱 步骤六：安装和使用

### 1. 安装APK
- 将APK文件传输到安卓手机
- 在手机设置中允许"安装未知来源应用"
- 点击APK文件进行安装

### 2. 首次使用
- 打开"房源监控"应用
- 授予通知权限
- 输入美团小程序地址
- 点击"开始监控"

## 🔍 构建状态监控

### 查看构建状态
- 🟢 **绿色**: 构建成功
- 🔴 **红色**: 构建失败
- 🟡 **黄色**: 构建进行中

### 构建徽章
在仓库README中会显示：
```
[![Android CI/CD](https://github.com/forcoder/house-monitor/actions/workflows/android-build.yml/badge.svg)](https://github.com/forcoder/house-monitor/actions/workflows/android-build.yml)
```

## 🚨 故障排除

### 问题1：推送失败
**错误**: `fatal: unable to access 'https://github.com/forcoder/house-monitor.git/'`
**解决**:
1. 检查网络连接
2. 确认仓库URL正确
3. 尝试使用SSH：`git@github.com:forcoder/house-monitor.git`
4. 配置Git凭证：`git config --global credential.helper wincred`

### 问题2：Actions未启用
**解决**:
1. 进入仓库Settings
2. 选择Actions → General
3. 启用Actions并保存

### 问题3：构建失败
**解决**:
1. 点击失败的构建查看日志
2. 检查错误信息
3. 根据错误修复代码
4. 重新推送触发构建

### 问题4：文件缺失
**解决**:
1. 检查是否遗漏了重要文件
2. 重新上传缺失的文件
3. 确保 `.github/workflows/` 目录存在

## 📞 获取帮助

### GitHub官方资源
- [GitHub帮助文档](https://docs.github.com/)
- [GitHub Actions文档](https://docs.github.com/actions)

### 项目特定问题
- 查看本项目的其他文档
- 在GitHub Issues中提问
- 检查已有的解决方案

## 🎯 预期结果

成功完成以上步骤后，你将拥有：

✅ **GitHub仓库**: https://github.com/forcoder/house-monitor  
✅ **自动构建**: GitHub Actions自动构建APK  
✅ **Release发布**: 自动创建带版本号的Release  
✅ **APK下载**: 可随时下载最新版本的APK  
✅ **持续集成**: 代码更新自动触发新构建  

## 🎉 开始吧！

按照以上步骤操作，你很快就能拥有自己的自动化构建系统！

**记住关键步骤**:
1. 创建GitHub仓库
2. 上传代码
3. 启用Actions
4. 触发构建
5. 下载APK

祝你成功！🏠✨