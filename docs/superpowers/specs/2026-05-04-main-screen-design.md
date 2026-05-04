# 主页面现代感重设计 — 设计方案

**日期：** 2026-05-04
**方案：** A — 极简现代风

## 设计目标

将房源监控主页面从标准 Material3 风格升级为更具现代感的极简风格，提升视觉层次和信息可读性。

## 设计方案

### 1. 主题系统扩展 (`Theme.kt` / `Color.kt`)

- **新增渐变色彩**：`DashboardGradientStart`（靛蓝 `#5C6BC0`）、`DashboardGradientEnd`（紫 `#7E57C2`）
- **新增 `AppShapes`**：统一圆角风格
  - `cardLarge` = 20dp（仪表盘卡片）
  - `cardMedium` = 16dp（房源卡片）
  - `cardSmall` = 12dp（小卡片）
  - `pill` = 50dp（药丸形标签）
  - `button` = 12dp（按钮/输入框）
  - `dialog` = 24dp（弹窗）
- **新增 `AppGradients`**：`dashboard`（线性渐变）、`cardSurface`（垂直渐变）
- **状态色优化**：使用更深层的绿色/橙色/红色提升对比度

### 2. MainScreen 顶部导航

- `TopAppBar` → `LargeTopAppBar`：标题更大（`headlineMedium`），副标题显示监控中房源数量
- 精简操作按钮：保留刷新和设置，移除冗余按钮
- FAB → `ExtendedFloatingActionButton`：带"添加房源"文字 + `Outlined.Add` 图标，使用 `AppShapes.button` 圆角
- 增加底部 80dp 留白避免 FAB 遮挡

### 3. StatusSummaryCard 仪表盘

- **渐变背景**：使用 `AppGradients.dashboard`（靛蓝→紫）替代浅色容器
- **超大数字**：36sp 大字体展示可订/无房/总计三个核心指标
- **分布进度条**：6dp 高的圆角进度条，绿/黄/橙三色可视化比例
- **次要统计行**：半透明背景上的 `SmallChip` 展示部分无房/待检查/监控中
- **提示条**：未检查时的引导提示

### 4. PropertyCard 房源卡片

- 保留用户已有的优秀设计（左侧状态色条、ElevatedCard、StatusTag）
- **操作按钮**：`IconButton` → `FilledTonalIconButton`，半透明背景更现代
- **状态标签**：`StatusIndicator` → `StatusPill`（药丸形圆角）
- **分割线**：`HorizontalDivider` 分隔内容与操作区
- **圆角统一**：使用 `AppShapes` 常量
- **删除弹窗**：增加 `AppShapes.dialog` 圆角

### 5. AddPropertyDialog 添加弹窗

- 使用 `DialogProperties(usePlatformDefaultWidth = false)` 让弹窗更宽
- 圆角升级到 24dp（`AppShapes.dialog`）
- 输入框/按钮统一使用 `AppShapes.button` 圆角
- 标题升级到 `headlineSmall` + `FontWeight.Bold`
- 示例区域用 `Surface` + `cardSmall` 圆角包裹

### 6. EmptyState 空状态

- 增大图标区域：120dp 圆形背景 + 56dp 图标（原 64dp 平铺）
- 标题加粗（`headlineSmall` + `FontWeight.Bold`）
- 描述文字增加换行说明
- 按钮改为 `Button` + `AppShapes.button` 圆角 + 更大内边距

## 文件变更

| 文件 | 变更内容 |
|------|---------|
| `ui/theme/Color.kt` | 新增渐变色彩和强调色 |
| `ui/theme/Theme.kt` | 新增 `AppShapes`、`AppGradients` |
| `ui/main/MainScreen.kt` | LargeTopAppBar、仪表盘卡片、空状态、FAB |
| `ui/property/PropertyCard.kt` | FilledTonalIconButton、Pill 标签、分割线 |
| `ui/property/AddPropertyDialog.kt` | 大圆角弹窗、统一输入框形状 |
