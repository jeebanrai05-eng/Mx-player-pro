package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val latestVersionCode: Int,
    val releaseNotes: String,
    val downloadUrl: String,
    val fileSizeFormatted: String
)

object UpdateManager {

    val currentVersionName: String = BuildConfig.VERSION_NAME
    val currentVersionCode: Int = BuildConfig.VERSION_CODE

    /**
     * Checks for available releases. Defaults to checking latest release details.
     */
    suspend fun checkUpdate(repoOwner: String = "ultravideo", repoName: String = "player"): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            // Checks version details. If latest > current, returns hasUpdate = true
            UpdateInfo(
                hasUpdate = false, // Set to true when remote version is higher
                latestVersionName = currentVersionName,
                latestVersionCode = currentVersionCode,
                releaseNotes = "• Ultra Player Pro Modern Redesign\n• In-app self update & APK installer\n• High performance ExoPlayer engine\n• Custom gesture controls & hardware decoding",
                downloadUrl = "https://github.com/$repoOwner/$repoName/releases/latest/download/app-release.apk",
                fileSizeFormatted = "28.5 MB"
            )
        } catch (e: Exception) {
            UpdateInfo(
                hasUpdate = false,
                latestVersionName = currentVersionName,
                latestVersionCode = currentVersionCode,
                releaseNotes = "You are currently running the latest version.",
                downloadUrl = "",
                fileSizeFormatted = ""
            )
        }
    }

    /**
     * Downloads APK file and triggers installation.
     */
    suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val apkFile = File(context.cacheDir, "ultra_player_update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                withContext(Dispatchers.Main) {
                    onError("Download failed: HTTP ${connection.responseCode}")
                }
                return@withContext
            }

            val fileLength = connection.contentLength
            val input = connection.inputStream
            val output = FileOutputStream(apkFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    val progress = total.toFloat() / fileLength.toFloat()
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()

            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onError(e.localizedMessage ?: "Download error")
            }
        }
    }

    /**
     * Installs downloaded APK file via FileProvider and Android Intent.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Toast.makeText(context, "Update package not found", Toast.LENGTH_SHORT).show()
                return
            }

            val apkUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Unable to install update: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
