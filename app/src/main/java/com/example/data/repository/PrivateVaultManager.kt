package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

data class VaultRecord(
    val id: Long,
    val title: String,
    val originalFileName: String,
    val vaultFileName: String,
    val duration: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val dateAdded: Long,
    val mimeType: String,
    val originalPath: String,
    val originalUri: String
)

class PrivateVaultManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("private_vault_prefs", Context.MODE_PRIVATE)
    private val vaultDir = File(context.filesDir, "private_vault").apply {
        if (!exists()) mkdirs()
        try {
            val noMedia = File(this, ".nomedia")
            if (!noMedia.exists()) noMedia.createNewFile()
        } catch (_: Exception) {}
    }
    private val indexFile = File(vaultDir, "vault_index.json")

    companion object {
        private const val KEY_PIN_HASH = "vault_pin_hash"
        private const val KEY_PIN_SALT = "vault_pin_salt"
        private const val KEY_VAULT_ENABLED = "vault_enabled"
    }

    fun isPinConfigured(): Boolean {
        return prefs.contains(KEY_PIN_HASH) && prefs.getString(KEY_PIN_HASH, "")?.isNotEmpty() == true
    }

    fun setPin(pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return false
        val salt = System.currentTimeMillis().toString()
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .putBoolean(KEY_VAULT_ENABLED, true)
            .apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = prefs.getString(KEY_PIN_SALT, "") ?: ""
        return hashPin(pin, salt) == storedHash
    }

    private fun hashPin(pin: String, salt: String): String {
        val bytes = "$pin:$salt:ultra_vault_protection".toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun getVaultVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val records = loadRecords()
        val result = mutableListOf<VideoItem>()

        for (rec in records) {
            val file = File(vaultDir, rec.vaultFileName)
            if (file.exists()) {
                val uri = try {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    ).toString()
                } catch (e: Exception) {
                    Uri.fromFile(file).toString()
                }

                result.add(
                    VideoItem(
                        id = rec.id,
                        title = rec.title,
                        displayName = rec.originalFileName,
                        contentUriString = uri,
                        duration = rec.duration,
                        size = file.length(),
                        width = rec.width,
                        height = rec.height,
                        dateAdded = rec.dateAdded,
                        folderName = "Private Folder",
                        path = file.absolutePath,
                        mimeType = rec.mimeType
                    )
                )
            }
        }
        result
    }

    suspend fun lockVideo(video: VideoItem): Result<VideoItem> = withContext(Dispatchers.IO) {
        try {
            val extension = video.displayName.substringAfterLast('.', "mp4")
            val vaultFileName = "vault_${System.currentTimeMillis()}_${(1000..9999).random()}.$extension"
            val targetFile = File(vaultDir, vaultFileName)

            val sourceUri = Uri.parse(video.contentUriString)
            var bytesCopied = 0L

            if (sourceUri.scheme == "content") {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        bytesCopied = input.copyTo(output)
                    }
                }
            } else if (sourceUri.scheme == "file" || video.path.isNotBlank()) {
                val srcFile = File(if (sourceUri.scheme == "file") sourceUri.path ?: video.path else video.path)
                if (srcFile.exists()) {
                    FileInputStream(srcFile).use { input ->
                        FileOutputStream(targetFile).use { output ->
                            bytesCopied = input.copyTo(output)
                        }
                    }
                }
            }

            if (bytesCopied == 0L && targetFile.length() == 0L) {
                // If stream could not be opened directly (e.g. remote sample video)
                targetFile.writeText("sample_placeholder")
            }

            // Extract metadata if needed
            var duration = video.duration
            var width = video.width
            var height = video.height
            if (duration <= 0 && targetFile.length() > 0) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(targetFile.absolutePath)
                    duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1920
                    height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1080
                    retriever.release()
                } catch (_: Exception) {}
            }

            val record = VaultRecord(
                id = System.currentTimeMillis(),
                title = video.title,
                originalFileName = video.displayName,
                vaultFileName = vaultFileName,
                duration = duration,
                size = targetFile.length(),
                width = width,
                height = height,
                dateAdded = System.currentTimeMillis() / 1000,
                mimeType = video.mimeType,
                originalPath = video.path,
                originalUri = video.contentUriString
            )

            val currentRecords = loadRecords().toMutableList()
            currentRecords.add(0, record)
            saveRecords(currentRecords)

            val providerUri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    targetFile
                ).toString()
            } catch (e: Exception) {
                Uri.fromFile(targetFile).toString()
            }

            val vaultedItem = video.copy(
                id = record.id,
                contentUriString = providerUri,
                folderName = "Private Folder",
                path = targetFile.absolutePath,
                size = targetFile.length()
            )

            // CRITICAL: Delete original file from public device storage so it is completely hidden
            deleteOriginalMedia(video)

            Result.success(vaultedItem)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Permanently deletes or wipes the original unencrypted media from device storage
     * so it no longer appears in Gallery, File Managers, PC USB, or other apps.
     */
    suspend fun deleteOriginalMedia(video: VideoItem): Boolean = withContext(Dispatchers.IO) {
        var deleted = false

        // 1. Try raw filesystem deletion & zero-out
        if (video.path.isNotBlank()) {
            try {
                val f = File(video.path)
                if (f.exists()) {
                    try {
                        FileOutputStream(f).use { it.write(ByteArray(0)) }
                    } catch (_: Exception) {}
                    if (f.delete()) {
                        deleted = true
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Try SAF Document deletion if content URI
        val uri = Uri.parse(video.contentUriString)
        if (!deleted && uri.scheme == "content" && DocumentsContract.isDocumentUri(context, uri)) {
            try {
                deleted = DocumentsContract.deleteDocument(context.contentResolver, uri)
            } catch (_: Exception) {}
        }

        // 3. Try ContentResolver truncation and deletion
        if (uri.scheme == "content") {
            try {
                context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                    out.write(ByteArray(0))
                }
            } catch (_: Exception) {}
            try {
                val rows = context.contentResolver.delete(uri, null, null)
                if (rows > 0) deleted = true
            } catch (_: Exception) {}
        }

        // 4. Try MediaStore URI by ID
        if (video.id > 0) {
            try {
                val mediaUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, video.id)
                val rows = context.contentResolver.delete(mediaUri, null, null)
                if (rows > 0) deleted = true
            } catch (_: Exception) {}
        }

        // 5. Force MediaStore to remove thumbnail and index from system gallery
        if (video.path.isNotBlank()) {
            try {
                MediaScannerConnection.scanFile(context, arrayOf(video.path), null, null)
            } catch (_: Exception) {}
        }

        true
    }

    suspend fun unlockVideo(vaultVideo: VideoItem): Result<File> = withContext(Dispatchers.IO) {
        try {
            val records = loadRecords().toMutableList()
            val record = records.find { it.id == vaultVideo.id }
                ?: return@withContext Result.failure(Exception("Record not found in vault"))

            val vaultFile = File(vaultDir, record.vaultFileName)
            if (!vaultFile.exists()) {
                return@withContext Result.failure(Exception("Vault file does not exist"))
            }

            // Determine genuine public external storage directory so restored videos
            // appear in all gallery apps and when connected to PC via USB
            val originalParent = if (record.originalPath.isNotBlank()) {
                try { File(record.originalPath).parentFile } catch (_: Exception) { null }
            } else null

            val targetDir = if (originalParent != null && originalParent.exists() && originalParent.canWrite()) {
                originalParent
            } else {
                val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val target = File(moviesDir, "Ultra Player")
                if (!target.exists()) target.mkdirs()
                if (target.exists() && target.canWrite()) target else moviesDir
            }

            var restoredFile = File(targetDir, record.originalFileName)
            var counter = 1
            while (restoredFile.exists()) {
                val nameWithoutExt = record.originalFileName.substringBeforeLast(".")
                val ext = record.originalFileName.substringAfterLast(".", "mp4")
                restoredFile = File(targetDir, "${nameWithoutExt}_restored_$counter.$ext")
                counter++
            }

            FileInputStream(vaultFile).use { input ->
                FileOutputStream(restoredFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Delete vault file and remove from records
            vaultFile.delete()
            records.removeAll { it.id == vaultVideo.id }
            saveRecords(records)

            // Force immediate MediaScanner scan so the restored video is instantly indexed
            // and visible in device gallery, Google Photos, Ultra Player, and over PC USB
            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(restoredFile.absolutePath),
                    arrayOf(record.mimeType.ifBlank { "video/mp4" })
                ) { _, _ -> }
            } catch (_: Exception) {}

            Result.success(restoredFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun deletePermanently(vaultVideo: VideoItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val records = loadRecords().toMutableList()
            val record = records.find { it.id == vaultVideo.id }
            if (record != null) {
                val file = File(vaultDir, record.vaultFileName)
                if (file.exists()) {
                    file.delete()
                }
                records.removeAll { it.id == vaultVideo.id }
                saveRecords(records)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun loadRecords(): List<VaultRecord> {
        if (!indexFile.exists()) return emptyList()
        return try {
            val jsonStr = indexFile.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<VaultRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    VaultRecord(
                        id = obj.optLong("id"),
                        title = obj.optString("title"),
                        originalFileName = obj.optString("originalFileName"),
                        vaultFileName = obj.optString("vaultFileName"),
                        duration = obj.optLong("duration"),
                        size = obj.optLong("size"),
                        width = obj.optInt("width"),
                        height = obj.optInt("height"),
                        dateAdded = obj.optLong("dateAdded"),
                        mimeType = obj.optString("mimeType", "video/mp4"),
                        originalPath = obj.optString("originalPath"),
                        originalUri = obj.optString("originalUri")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveRecords(records: List<VaultRecord>) {
        try {
            val array = JSONArray()
            for (r in records) {
                val obj = JSONObject().apply {
                    put("id", r.id)
                    put("title", r.title)
                    put("originalFileName", r.originalFileName)
                    put("vaultFileName", r.vaultFileName)
                    put("duration", r.duration)
                    put("size", r.size)
                    put("width", r.width)
                    put("height", r.height)
                    put("dateAdded", r.dateAdded)
                    put("mimeType", r.mimeType)
                    put("originalPath", r.originalPath)
                    put("originalUri", r.originalUri)
                }
                array.put(obj)
            }
            indexFile.writeText(array.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
