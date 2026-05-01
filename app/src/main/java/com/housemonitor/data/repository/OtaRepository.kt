package com.housemonitor.data.repository

import android.content.Context
import com.housemonitor.data.model.OtaUpdate
import com.housemonitor.data.remote.ShzlApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface OtaRepository {
    suspend fun checkForUpdate(currentVersionCode: Int): Result<OtaUpdate?>
    val downloadProgress: Flow<Float>
    suspend fun downloadApk(update: OtaUpdate): Result<String>
    suspend fun getDownloadedApkPath(versionCode: Int): String?
    suspend fun cleanupOldVersions()
}

@Singleton
class OtaRepositoryImpl @Inject constructor(
    private val context: Context,
    private val shzlApiService: ShzlApiService
) : OtaRepository {

    private val _downloadProgress = MutableStateFlow(0f)
    override val downloadProgress: StateFlow<Float> = _downloadProgress

    override suspend fun checkForUpdate(currentVersionCode: Int): Result<OtaUpdate?> {
        return try {
            val versionInfo = shzlApiService.getVersionInfo()
            if (versionInfo.versionCode > currentVersionCode) {
                Result.success(versionInfo.toOtaUpdate())
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(Exception("检查更新失败: ${e.message}", e))
        }
    }

    override suspend fun downloadApk(update: OtaUpdate): Result<String> {
        return try {
            val otaDir = File(context.getExternalFilesDir(null), "Updates")
            if (!otaDir.exists()) otaDir.mkdirs()

            val fileName = "house-monitor-v${update.versionName}-${update.versionCode}.apk"
            val targetFile = File(otaDir, fileName)

            val response = shzlApiService.downloadApk()
            response.byteStream().use { input ->
                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var totalBytesRead = 0L
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (update.fileSize > 0) {
                            _downloadProgress.value = totalBytesRead.toFloat() / update.fileSize
                        }
                    }
                }
            }
            _downloadProgress.value = 1f
            Result.success(targetFile.absolutePath)
        } catch (e: Exception) {
            _downloadProgress.value = 0f
            Result.failure(Exception("下载失败: ${e.message}", e))
        }
    }

    override suspend fun getDownloadedApkPath(versionCode: Int): String? {
        val otaDir = File(context.getExternalFilesDir(null), "Updates")
        val files = otaDir.listFiles() ?: return null
        return files.find { it.name.contains("v$versionCode") && it.name.endsWith(".apk") }?.absolutePath
    }

    override suspend fun cleanupOldVersions() {
        try {
            val otaDir = File(context.getExternalFilesDir(null), "Updates")
            otaDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".apk")) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }
}
