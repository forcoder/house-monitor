# 房源监控 APP — 多平台支持 + 审计历史 设计文档

> 参考: csBaby 项目的 OTA 实现、现有 PlatformParser 架构

## 目标

1. 新增三个平台支持：途家 (Tujia)、小猪民宿 (Xiaozhu)、木鸟民宿 (Muniao)
2. 房源链接自动识别平台 + 手动确认/切换
3. 每房源保留最近 10 条监控记录，支持清理
4. 变化详情追踪：日期 + 数量级别，记录每次检查与上次的变化差异
5. 历史记录 UI：时间轴 + 对比视图，展开查看变化详情
6. 增强通知内容，显示变化前后状态

## 架构

```
URL 输入 → PlatformParserFactory.detectPlatform() → 自动选择 parser
         ↓ 用户可手动覆盖
PlatformParser (interface) ← MeituanPlatformParser (已有)
                           ← TujiaPlatformParser (新增)
                           ← XiaozhuPlatformParser (新增)
                           ← MuniaoPlatformParser (新增)

PropertyMonitorWorker → 计算 ChangeSummary → 保存 MonitorRecord
                      → 发送增强通知

HistoryScreen → 时间轴视图 + 展开对比详情
```

## 平台检测规则

| 平台 | platformId | URL 匹配规则 |
|------|-----------|-------------|
| 美团民宿 | meituan | url.contains("meituan.com") |
| 途家 | tujia | url.contains("tujia.com") |
| 小猪民宿 | xiaozhu | url.contains("xiaozhu.com") |
| 木鸟民宿 | muniao | url.contains("muniao.com") |

## 数据模型变更

### MonitorRecord 新增字段

```kotlin
data class MonitorRecord(
    // ... 现有字段不变
    val changeSummary: String = ""  // JSON: {newlyUnavailable: [], newlyAvailable: [], changeType: ""})
)
```

changeSummary JSON 格式：
```json
{
  "newlyUnavailable": ["2026-05-10", "2026-05-11"],
  "newlyAvailable": ["2026-05-08"],
  "changeType": "BECAME_UNAVAILABLE"
}
```

### Room 数据库迁移

version 2 → version 3：ALTER TABLE monitor_records ADD COLUMN changeSummary TEXT NOT NULL DEFAULT ''

## DAO 新增查询

```kotlin
// 获取某房源最近 N 条记录
@Query("SELECT * FROM monitor_records WHERE propertyId = :propertyId ORDER BY checkedAt DESC LIMIT :limit")
suspend fun getRecentRecordsByPropertyId(propertyId: String, limit: Int = 10): List<MonitorRecord>

// 清理旧记录（每房源只保留最近 10 条）
@Query("DELETE FROM monitor_records WHERE propertyId = :propertyId AND id NOT IN (SELECT id FROM monitor_records WHERE propertyId = :propertyId ORDER BY checkedAt DESC LIMIT 10)")
suspend fun cleanupOldRecordsByPropertyId(propertyId: String)
```

## 变化检测逻辑

在 `monitorSingleProperty()` 中：
1. 获取当前不可用日期列表
2. 获取最近一次成功记录的不可用日期列表
3. 计算 diff：
   - `newlyUnavailable = current - previous`
   - `newlyAvailable = previous - current`
   - changeType: `BECAME_UNAVAILABLE` / `BECAME_AVAILABLE` / `PARTIAL_CHANGE` / `NO_CHANGE`
4. 序列化为 JSON 存入 `changeSummary`
5. 保存记录后调用 cleanup

## 通知增强

NotificationManager 新增方法：
```kotlin
fun showDetailedChangeNotification(
    propertyName: String,
    changeSummary: ChangeSummary
)
```

通知内容格式：
- "房源A：2个日期变为无房（5/10, 5/11）"
- "房源A：1个日期变为有房（5/8）"

## UI 变更

### AddPropertyDialog
- URL 输入时实时检测平台（显示平台图标/标签）
- 平台下拉列表新增：途家、小猪民宿、木鸟民宿
- 自动检测结果可手动覆盖

### PropertyCard
- 显示平台标签（美团民宿/途家/小猪民宿/木鸟民宿）

### MonitorRecordCard（重新设计）
- 时间轴节点样式
- 显示 changeType 图标和摘要
- 可展开查看变化详情列表

### HistoryScreen
- 筛选器支持按平台筛选
- 时间轴视图
- 每个记录卡片展开后显示：新增无房日期、释放有房日期

## 版本计划

- versionCode: 4
- versionName: "1.2.0"
