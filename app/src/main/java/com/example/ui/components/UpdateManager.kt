package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val latestVersionCode: Int,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val sha256Url: String,
    val sha256Checksum: String,
    val fileSizeFormatted: String
)

object UpdateManager {

    val currentVersionName: String = BuildConfig.VERSION_NAME
    val currentVersionCode: Int = BuildConfig.VERSION_CODE

    // Default target repository
    const val DEFAULT_REPO_OWNER = "jeebanrai05-eng"
    const val DEFAULT_REPO_NAME = "Mx-player-pro"

    /**
     * Checks for available releases from the GitHub Releases API.
     */
    suspend fun checkUpdate(
        repoOwner: String = DEFAULT_REPO_OWNER,
        repoName: String = DEFAULT_REPO_NAME
    ): UpdateInfo = withContext(Dispatchers.IO) {
        val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
        var connection: HttpURLConnection? = null
        try {
            val url = URL(apiUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "UltraVideoPlayer-Android")
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val jsonString = reader.use { it.readText() }
                val releaseJson = JSONObject(jsonString)

                val tagName = releaseJson.optString("tag_name", "").removePrefix("v").trim()
                val releaseName = releaseJson.optString("name", "Release v$tagName")
                val releaseBody = releaseJson.optString("body", "Bug fixes and performance improvements.")

                var apkDownloadUrl = ""
                var sha256DownloadUrl = ""
                var apkSizeBytes: Long = 0

                val assets = releaseJson.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        val downloadUrl = asset.optString("browser_download_url", "")
                        val size = asset.optLong("size", 0L)

                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = downloadUrl
                            apkSizeBytes = size
                        } else if (name.endsWith(".sha256", ignoreCase = true) || name.endsWith(".sha256sum", ignoreCase = true)) {
                            sha256DownloadUrl = downloadUrl
                        }
                    }
                }

                // If direct APK asset wasn't found in assets, fallback to releases latest download pattern
                if (apkDownloadUrl.isEmpty()) {
                    apkDownloadUrl = "https://github.com/$repoOwner/$repoName/releases/latest/download/app-release.apk"
                }

                val remoteVersionClean = tagName.split("-")[0].trim()
                val isNewer = isRemoteVersionHigher(remoteVersionClean, currentVersionName)

                val formattedSize = if (apkSizeBytes > 0) {
                    String.format("%.1f MB", apkSizeBytes / (1024f * 1024f))
                } else {
                    "~28 MB"
                }

                UpdateInfo(
                    hasUpdate = isNewer,
                    latestVersionName = if (tagName.isNotEmpty()) tagName else currentVersionName,
                    latestVersionCode = currentVersionCode + if (isNewer) 1 else 0,
                    releaseTitle = releaseName,
                    releaseNotes = releaseBody,
                    downloadUrl = apkDownloadUrl,
                    sha256Url = sha256DownloadUrl,
                    sha256Checksum = "",
                    fileSizeFormatted = formattedSize
                )
            } else {
                // If repository release has not been published yet or 404
                UpdateInfo(
                    hasUpdate = false,
                    latestVersionName = currentVersionName,
                    latestVersionCode = currentVersionCode,
                    releaseTitle = "Current Version",
                    releaseNotes = "You are running the latest version (v$currentVersionName).",
                    downloadUrl = "https://github.com/$repoOwner/$repoName/releases/latest/download/app-release.apk",
                    sha256Url = "",
                    sha256Checksum = "",
                    fileSizeFormatted = ""
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateInfo(
                hasUpdate = false,
                latestVersionName = currentVersionName,
                latestVersionCode = currentVersionCode,
                releaseTitle = "Network Notice",
                releaseNotes = "Unable to connect to GitHub releases. Current version: v$currentVersionName",
                downloadUrl = "https://github.com/$repoOwner/$repoName/releases/latest/download/app-release.apk",
                sha256Url = "",
                sha256Checksum = "",
                fileSizeFormatted = ""
            )
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Compares two semantic version strings (e.g. "1.1.0" > "1.0.0").
     */
    private fun isRemoteVersionHigher(remote: String, current: String): Boolean {
        try {
            val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
        } catch (_: Exception) {
            // fallback string comparison if format is non-standard
            return remote.compareTo(current) > 0
        }
        return false
    }

    /**
     * Downloads APK file, calculates & verifies SHA-256 hash, validates package integrity,
     * and triggers secure Android installation via FileProvider.
     */
    suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        sha256Url: String = "",
        expectedSha256: String = "",
        onProgress: (Float) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        var apkFile: File? = null
        try {
            apkFile = File(context.cacheDir, "ultra_player_update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            // 1. Download the APK binary
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 20000
            connection.readTimeout = 20000
            connection.connect()

            // Handle possible redirects
            var realConnection = connection
            if (connection.responseCode in listOf(HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, 307, 308)) {
                val newUrl = connection.getHeaderField("Location")
                if (!newUrl.isNullOrEmpty()) {
                    realConnection = URL(newUrl).openConnection() as HttpURLConnection
                    realConnection.connectTimeout = 20000
                    realConnection.readTimeout = 20000
                    realConnection.connect()
                }
            }

            if (realConnection.responseCode != HttpURLConnection.HTTP_OK) {
                withContext(Dispatchers.Main) {
                    onError("Download failed: HTTP ${realConnection.responseCode}")
                }
                return@withContext
            }

            val fileLength = realConnection.contentLength
            val input = realConnection.inputStream
            val output = FileOutputStream(apkFile)

            val data = ByteArray(8192)
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

            // 2. Compute SHA-256 hash of the downloaded APK
            val calculatedSha256 = calculateFileSha256(apkFile)

            // 3. Verify SHA-256 Checksum if provided directly or via sha256Url
            var targetChecksum = expectedSha256.trim()
            if (targetChecksum.isEmpty() && sha256Url.isNotEmpty()) {
                targetChecksum = fetchRemoteChecksum(sha256Url)
            }

            if (targetChecksum.isNotEmpty()) {
                // Checksums in files often look like: "<hash>  filename.apk" or just "<hash>"
                val cleanExpectedHash = targetChecksum.split(Regex("\\s+"))[0].trim()
                if (!calculatedSha256.equals(cleanExpectedHash, ignoreCase = true)) {
                    apkFile.delete()
                    withContext(Dispatchers.Main) {
                        onError("SHA-256 Checksum mismatch! Verification failed.\nExpected: $cleanExpectedHash\nComputed: $calculatedSha256")
                    }
                    return@withContext
                }
            }

            // 4. Validate APK package integrity using Android PackageManager
            val packageManager = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            }

            if (packageInfo == null) {
                apkFile.delete()
                withContext(Dispatchers.Main) {
                    onError("Downloaded package is corrupted or invalid APK.")
                }
                return@withContext
            }

            if (packageInfo.packageName != context.packageName) {
                apkFile.delete()
                withContext(Dispatchers.Main) {
                    onError("Security Error: Package ID mismatch (${packageInfo.packageName} != ${context.packageName}).")
                }
                return@withContext
            }

            // 5. Securely trigger system package installer
            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onError(e.localizedMessage ?: "Download or verification error")
            }
        }
    }

    /**
     * Calculates the SHA-256 hexadecimal digest of a local file.
     */
    fun calculateFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        val sb = StringBuilder()
        for (b in hashBytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    /**
     * Downloads and parses checksum string from a remote URL.
     */
    private fun fetchRemoteChecksum(sha256Url: String): String {
        return try {
            val url = URL(sha256Url)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.connect()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                text.trim()
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
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

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

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
