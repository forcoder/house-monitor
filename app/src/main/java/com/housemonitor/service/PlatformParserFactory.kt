package com.housemonitor.service

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlatformParserFactory @Inject constructor() {
    private val parsers: List<PlatformParser> = listOf(
        MeituanPlatformParser(),
        TujiaPlatformParser(),
        XiaozhuPlatformParser(),
        MuniaoPlatformParser()
    )

    fun getParser(platformId: String): PlatformParser {
        return parsers.find { it.platformId == platformId }
            ?: parsers.first()
    }

    fun detectPlatform(url: String): String {
        return parsers.find { it.matchesUrl(url) }?.platformId ?: "meituan"
    }

    fun getAllPlatforms(): List<PlatformParser> = parsers
}
