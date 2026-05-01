package com.housemonitor.data.remote

import com.housemonitor.data.model.OtaUpdate
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Streaming
import okhttp3.ResponseBody

interface ShzlApiService {
    @GET("~${ShzlConfig.VERSION_FILE_NAME}")
    suspend fun getVersionInfo(): ShzlVersionInfo

    @Streaming
    @GET("~${ShzlConfig.APK_FILE_NAME}")
    suspend fun downloadApk(): ResponseBody
}

data class ShzlVersionInfo(
    @SerializedName("versionCode") val versionCode: Int,
    @SerializedName("versionName") val versionName: String,
    @SerializedName("releaseDate") val releaseDate: String,
    @SerializedName("fileSize") val fileSize: Long,
    @SerializedName("md5") val md5: String,
    @SerializedName("downloadUrl") val downloadUrl: String,
    @SerializedName("releaseNotes") val releaseNotes: String,
    @SerializedName("forceUpdate") val forceUpdate: Boolean = false
) {
    fun toOtaUpdate(): OtaUpdate {
        return OtaUpdate(
            versionCode = versionCode,
            versionName = versionName,
            downloadUrl = downloadUrl,
            fileSize = fileSize,
            md5 = md5,
            releaseNotes = releaseNotes,
            releaseDate = releaseDate,
            isForceUpdate = forceUpdate,
            minRequiredVersion = 0
        )
    }
}
