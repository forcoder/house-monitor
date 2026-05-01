package com.housemonitor.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "properties")
data class Property(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val platform: String = "meituan",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastCheckedAt: Long = 0L,
    val description: String = ""
)