package com.housemonitor.data.model

enum class UpdateStatus {
    IDLE, CHECKING, UPDATE_AVAILABLE, DOWNLOADING, DOWNLOADED, INSTALLING, SUCCESS, FAILED
}

data class OtaUpdate(
    val versionCode: Int = 0,
    val versionName: String = "",
    val downloadUrl: String = "",
    val fileSize: Long = 0,
    val md5: String = "",
    val releaseNotes: String = "",
    val releaseDate: String = "",
    val isForceUpdate: Boolean = false,
    val minRequiredVersion: Int = 0
) {
    fun needsUpdate(currentVersionCode: Int): Boolean {
        return versionCode > currentVersionCode
    }
}
