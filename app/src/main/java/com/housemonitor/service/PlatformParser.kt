package com.housemonitor.service

/**
 * 平台解析器接口 — 不同平台实现各自的日历状态检测逻辑
 */
interface PlatformParser {
    /** 平台标识 */
    val platformId: String

    /** 平台显示名称 */
    val platformName: String

    /** 生成注入到 WebView 的 JS 代码，用于检测无房日期 */
    fun buildCalendarDetectionJs(): String

    /** 平台匹配的 URL 规则（用于自动识别平台） */
    fun matchesUrl(url: String): Boolean
}
