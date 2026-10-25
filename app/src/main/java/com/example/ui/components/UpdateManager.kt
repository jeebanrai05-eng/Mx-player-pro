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
import org.json.JSONArray
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
    val fileSizeFormatted: String,
    val isPrivateRepoError: Boolean = false
)

object UpdateManager {

    val currentVersionName: String = BuildConfig.VERSION_NAME
    val currentVersionCode: Int = BuildConfig.VERSION_CODE

    // Default target repository
    const val DEFAULT_REPO_OWNER = "jeebanrai05-eng"
    const val DEFAULT_REPO_NAME = "Mx-player-pro"

    /**
     * Checks for available releases from the GitHub Releases API or from a direct tag/APK link.
     */
    suspend fun checkUpdate(
        repoOwner: String = DEFAULT_REPO_OWNER,
        repoName: String = DEFAULT_REPO_NAME,
        githubToken: String = "",
        customReleaseOrApkUrl: String = ""
    ): UpdateInfo = withContext(Dispatchers.IO) {
        // Case 1: Custom Release Tag or Direct APK URL provided
        if (customReleaseOrApkUrl.isNotBlank()) {
            val trimmedUrl = customReleaseOrApkUrl.trim()
            if (trimmedUrl.endsWith(".apk", ignoreCase = true) || trimmedUrl.contains(".apk?")) {
                return@withContext UpdateInfo(
                    hasUpdate = true,
                    latestVersionName = "Custom Release",
                    latestVersionCode = currentVersionCode + 1,
                    releaseTitle = "Custom APK Update",
                    releaseNotes = "Target URL: $trimmedUrl",
                    downloadUrl = trimmedUrl,
                    sha256Url = "",
                    sha256Checksum = "",
                    fileSizeFormatted = "~28 MB"
                )
            } else if (trimmedUrl.contains("releases/tag/")) {
                val tagRegex = Regex("releases/tag/([^/?#]+)")
                val match = tagRegex.find(trimmedUrl)
                val tagName = match?.groupValues?.get(1) ?: "v1.0.0"
                val apkUrl = "https://github.com/$repoOwner/$repoName/releases/download/$tagName/app-release.apk"
                val shaUrl = "https://github.com/$repoOwner/$repoName/releases/download/$tagName/app-release.apk.sha256"
                return@withContext UpdateInfo(
                    hasUpdate = true,
                    latestVersionName = tagName,
                    latestVersionCode = currentVersionCode + 1,
                    releaseTitle = "Release $tagName",
                    releaseNotes = "Loaded from release tag: $tagName.\nDirect download: app-release.apk",
                    downloadUrl = apkUrl,
                    sha256Url = shaUrl,
                    sha256Checksum = "",
                    fileSizeFormatted = "~28 MB"
                )
            }
        }

        // Case 2: Query GitHub Releases API
        val latestUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
        val releasesListUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases"

        var releaseJson: JSONObject? = null
        var isPrivateOr404 = false

        // Attempt 1: Fetch releases/latest
        try {
            val response = fetchGithubJson(latestUrl, githubToken)
            if (response != null) {
                releaseJson = JSONObject(response)
            }
        } catch (_: Exception) {}

        // Attempt 2: If latest is 404, fallback to /releases list (newest is index 0)
        if (releaseJson == null) {
            try {
                val response = fetchGithubJson(releasesListUrl, githubToken)
                if (response != null) {
                    val array = JSONArray(response)
                    if (array.length() > 0) {
                        releaseJson = array.getJSONObject(0)
                    }
                } else {
                    isPrivateOr404 = true
                }
            } catch (_: Exception) {
                isPrivateOr404 = true
            }
        }

        if (releaseJson != null) {
            val rawTag = releaseJson.optString("tag_name", "").trim()
            val tagName = rawTag.removePrefix("v").trim()
            val releaseName = releaseJson.optString("name", "Release $rawTag")
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

            if (apkDownloadUrl.isEmpty()) {
                apkDownloadUrl = "https://github.com/$repoOwner/$repoName/releases/download/$rawTag/app-release.apk"
            }

            // Check if newer by semantic version or build number (e.g. v1.0.0-build.4 > build 1)
            val isNewer = isVersionOrBuildHigher(rawTag, currentVersionName, currentVersionCode)

            val formattedSize = if (apkSizeBytes > 0) {
                String.format("%.1f MB", apkSizeBytes / (1024f * 1024f))
            } else {
                "~28 MB"
            }

            UpdateInfo(
                hasUpdate = isNewer,
                latestVersionName = if (rawTag.isNotEmpty()) rawTag else currentVersionName,
                latestVersionCode = currentVersionCode + if (isNewer) 1 else 0,
                releaseTitle = releaseName,
                releaseNotes = releaseBody,
                downloadUrl = apkDownloadUrl,
                sha256Url = sha256DownloadUrl,
                sha256Checksum = "",
                fileSizeFormatted = formattedSize,
                isPrivateRepoError = false
            )
        } else {
            // Repo is private or release not found
            val explanation = if (isPrivateOr404) {
                "Notice: Repository '$repoOwner/$repoName' is Private (HTTP 404).\n\nGitHub does not permit anonymous release downloads for private repositories.\n\nTo enable automatic updates:\n1. In GitHub, go to Settings -> General -> Danger Zone -> Change repository visibility -> Make public.\n2. Or paste your release tag link (e.g. https://github.com/$repoOwner/$repoName/releases/tag/v1.0.0-build.4) in the custom link field below."
            } else {
                "You are running the latest version (v$currentVersionName)."
            }

            UpdateInfo(
                hasUpdate = false,
                latestVersionName = currentVersionName,
                latestVersionCode = currentVersionCode,
                releaseTitle = if (isPrivateOr404) "Private Repository Notice" else "Current Version",
                releaseNotes = explanation,
                downloadUrl = "", // Empty so users don't trigger broken 404 downloads
                sha256Url = "",
                sha256Checksum = "",
                fileSizeFormatted = "",
                isPrivateRepoError = isPrivateOr404
            )
        }
    }

    private fun fetchGithubJson(apiUrl: String, token: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(apiUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "UltraVideoPlayer-Android")
            if (token.isNotBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                reader.use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Compares version string and build number.
     * E.g. "v1.0.0-build.4" vs currentVersionName="1.0", currentVersionCode=1 -> TRUE
     */
    fun isVersionOrBuildHigher(remoteTag: String, currentVersionName: String, currentVersionCode: Int): Boolean {
        try {
            // Extract build number if present (e.g. -build.4 or build4)
            val buildRegex = Regex("""build[._]?(\d+)""", RegexOption.IGNORE_CASE)
            val buildMatch = buildRegex.find(remoteTag)
            if (buildMatch != null) {
                val remoteBuild = buildMatch.groupValues[1].toIntOrNull()
                if (remoteBuild != null && remoteBuild > currentVersionCode) {
                    return true
                }
            }

            // Semantic version check
            val cleanRemote = remoteTag.removePrefix("v").split("-")[0].trim()
            val cleanCurrent = currentVersionName.removePrefix("v").split("-")[0].trim()

            val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
        } catch (_: Exception) {
            return remoteTag.compareTo(currentVersionName) > 0
        }
        return false
    }

    /**
     * Downloads APK file with multi-hop redirect resolution, verifies SHA-256 hash,
     * validates package integrity, and triggers secure installation.
     */
    suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        sha256Url: String = "",
        expectedSha256: String = "",
        githubToken: String = "",
        onProgress: (Float) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (downloadUrl.isBlank()) {
            withContext(Dispatchers.Main) {
                onError("No download URL provided. Check updates first or enter a release link.")
            }
            return@withContext
        }

        var apkFile: File? = null
        try {
            apkFile = File(context.cacheDir, "ultra_player_update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            // 1. Follow HTTP redirects manually (GitHub downloads redirect 302 -> S3/Azure storage)
            var currentUrl = downloadUrl
            var connection: HttpURLConnection? = null
            var redirectCount = 0
            val maxRedirects = 6

            while (redirectCount < maxRedirects) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 25000
                connection.readTimeout = 25000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; UltraVideoPlayer)")
                connection.setRequestProperty("Accept", "*/*")

                // Only send GitHub Token to GitHub domains, never to AWS S3/Azure blob (prevents 400 Bad Request)
                if (githubToken.isNotBlank() && currentUrl.contains("github.com") && !currentUrl.contains("objects.githubusercontent.com")) {
                    connection.setRequestProperty("Authorization", "Bearer $githubToken")
                }

                connection.connect()
                val code = connection.responseCode

                if (code in listOf(HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, HttpURLConnection.HTTP_SEE_OTHER, 307, 308)) {
                    val location = connection.getHeaderField("Location")
                    if (location.isNullOrEmpty()) break
                    currentUrl = location
                    connection.disconnect()
                    redirectCount++
                } else {
                    break
                }
            }

            val finalConnection = connection
            if (finalConnection == null || finalConnection.responseCode != HttpURLConnection.HTTP_OK) {
                val code = finalConnection?.responseCode ?: -1
                val message = if (code == 404) {
                    "Download failed: HTTP 404 (Not Found).\n\nIf the repository 'jeebanrai05-eng/Mx-player-pro' is Private, GitHub denies access to release files. Please make the repository Public in GitHub Settings (Settings -> Danger Zone -> Change repository visibility -> Make public)."
                } else {
                    "Download failed: HTTP $code"
                }
                withContext(Dispatchers.Main) {
                    onError(message)
                }
                return@withContext
            }

            val fileLength = finalConnection.contentLength
            val input = finalConnection.inputStream
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
            finalConnection.disconnect()

            // 2. Compute SHA-256 hash of the downloaded APK
            val calculatedSha256 = calculateFileSha256(apkFile)

            // 3. Verify SHA-256 Checksum if available
            var targetChecksum = expectedSha256.trim()
            if (targetChecksum.isEmpty() && sha256Url.isNotEmpty()) {
                targetChecksum = fetchRemoteChecksum(sha256Url, githubToken)
            }

            if (targetChecksum.isNotEmpty()) {
                val cleanExpectedHash = targetChecksum.split(Regex("\\s+"))[0].trim()
                if (!calculatedSha256.equals(cleanExpectedHash, ignoreCase = true)) {
                    apkFile.delete()
                    withContext(Dispatchers.Main) {
                        onError("SHA-256 Checksum mismatch!\nExpected: $cleanExpectedHash\nComputed: $calculatedSha256")
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
                    onError("Downloaded file is corrupted or not a valid Android APK.")
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

    private fun fetchRemoteChecksum(sha256Url: String, token: String): String {
        return try {
            val url = URL(sha256Url)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            if (token.isNotBlank() && sha256Url.contains("github.com") && !sha256Url.contains("objects.githubusercontent.com")) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
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
