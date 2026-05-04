package com.housemonitor.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "monitor_records",
    foreignKeys = [ForeignKey(
        entity = Property::class,
        parentColumns = ["id"],
        childColumns = ["propertyId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class MonitorRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val propertyId: String,
    val checkDate: String, // yyyy-MM-dd格式
    val unavailableDates: String, // JSON格式存储日期列表
    val checkedAt: Long = System.currentTimeMillis(),
    val status: String = "success", // success, failed, timeout
    val changeSummary: String = "" // JSON: {newlyUnavailable, newlyAvailable, changeType}
)