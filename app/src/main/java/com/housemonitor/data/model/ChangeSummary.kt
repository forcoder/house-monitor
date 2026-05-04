package com.housemonitor.data.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * 记录两次检查之间的变化摘要
 */
data class ChangeSummary(
    @SerializedName("newlyUnavailable") val newlyUnavailable: List<String> = emptyList(),
    @SerializedName("newlyAvailable") val newlyAvailable: List<String> = emptyList(),
    @SerializedName("changeType") val changeType: String = "NO_CHANGE"
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): ChangeSummary {
            return try {
                Gson().fromJson(json, ChangeSummary::class.java)
            } catch (e: Exception) {
                ChangeSummary()
            }
        }

        fun noChange() = ChangeSummary(changeType = "NO_CHANGE")
    }
}

enum class ChangeType {
    NO_CHANGE,           // 无变化
    BECAME_UNAVAILABLE,  // 新增无房日期
    BECAME_AVAILABLE,    // 释放有房日期
    PARTIAL_CHANGE       // 部分日期变化（同时有新增和释放）
}
