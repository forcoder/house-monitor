# 房源监控 APP — shz.al 发布 + OTA 升级 实施计划

> 参考 csBaby 项目的 release.yml + OTA 实现

**Goal:** 
1. 将 APK 发布到 shz.al（版本信息 JSON + APK 文件）
2. 实现 OTA 升级功能（检查更新 → 下载 APK → 安装）

**Architecture:**
- CI/CD: GitHub Actions release 工作流打 tag 时触发，构建签名 APK，上传 shz.al
- OTA: ShzlConfig（配置）→ ShzlApiService（Retrofit）→ OtaRepository（仓库）→ OtaManager（管理器）→ UI（设置页）
- 版本信息 JSON 格式与 csBaby 一致，shz.al 短链接分发

---

## 前置条件

需要你提供 shz.al 的上传密码，用于配置 GitHub Secret `SHZAL_PASSWORD`。

---

### Task A: 配置 shz.al + GitHub Secrets

- 获取 shz.al 上传密码
- 在 GitHub 仓库 Settings → Secrets → Actions 中添加 `SHZAL_PASSWORD`

### Task B: 创建 ShzlConfig 和 ShzlApiService

参考 csBaby 的 `ShzlConfig.kt` + `ShzlApiService.kt`。

### Task C: 创建 OtaUpdate 数据模型和 UpdateStatus

参考 csBaby 的 `OtaUpdate.kt`。

### Task D: 创建 OtaRepository 和 OtaManager

参考 csBaby 的 `OtaRepository.kt` + `OtaManager.kt`。

### Task E: 添加 OTA 权限 + FileProvider 到 AndroidManifest

### Task F: 添加 Retrofit/OkHttp 依赖 + DI 模块

### Task G: 在设置页添加 OTA 升级 UI

### Task H: 更新 GitHub Actions release 工作流（shz.al 上传）

### Task I: 更新版本号 + 打 tag 触发 release
