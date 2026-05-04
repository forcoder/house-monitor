package com.housemonitor.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1, // 单例模式
    val checkInterval: Int = 5, // 检查间隔（分钟）
    val notificationEnabled: Boolean = true,
    val quietHoursStart: Int = 22, // 免打扰开始时间（小时）
    val quietHoursEnd: Int = 8, // 免打扰结束时间（小时）
    val autoRefreshEnabled: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)