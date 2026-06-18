package com.kinly.famapp.features.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** Содержимое version.json в публичном бакете Supabase. */
@Serializable
data class AppUpdateInfo(
    @SerialName("versionCode") val versionCode: Int,
    @SerialName("versionName") val versionName: String = "",
    @SerialName("apkUrl") val apkUrl: String,
    @SerialName("notes") val notes: String? = null
)

@Singleton
class UpdateRepository @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    /** Скачивает и парсит version.json. */
    suspend fun fetchManifest(manifestUrl: String): AppUpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(manifestUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = "GET"
            }
            conn.inputStream.bufferedReader().use { it.readText() }
                .let { json.decodeFromString<AppUpdateInfo>(it) }
        }.getOrNull()
    }

    /**
     * Скачивает APK в кэш и отдаёт файл. onProgress — доля 0..1 (или -1 если размер неизвестен).
     */
    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                requestMethod = "GET"
            }
            conn.connect()
            val total = conn.contentLength.toLong()
            val target = File(context.externalCacheDir ?: context.cacheDir, "update.apk")
            if (target.exists()) target.delete()

            conn.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) onProgress((downloaded.toFloat() / total).coerceIn(0f, 1f))
                        else onProgress(-1f)
                    }
                }
            }
            target
        }.getOrNull()
    }

    companion object {
        /** Запускает системный установщик для скачанного APK. */
        fun installApk(context: Context, file: File) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
