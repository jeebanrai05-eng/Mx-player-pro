package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.data.local.AppDatabase
import com.example.data.local.VideoEntity
import com.example.data.model.FolderItem
import com.example.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class VideoRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val videoDao = db.videoDao()
    val vaultManager = PrivateVaultManager(context)

    private val deletedUrisFlow = MutableStateFlow<Set<String>>(emptySet())
    private val refreshTrigger = MutableStateFlow(0)

    // Bundled public domain & creative commons test videos for emulator verification
    val sampleVideos = listOf(
        VideoItem(
            id = -101L,
            title = "Camera Capture (1080p)",
            displayName = "VID_20261024_camera.mp4",
            contentUriString = "https://raw.githubusercontent.com/bower-media-samples/big-buck-bunny-1080p-30s/master/video.mp4",
            duration = 30000L,
            size = 22718509L,
            width = 1920,
            height = 1080,
            dateAdded = System.currentTimeMillis() / 1000,
            folderName = "Camera",
            path = "https://raw.githubusercontent.com/bower-media-samples/big-buck-bunny-1080p-30s/master/video.mp4",
            mimeType = "video/mp4"
        ),
        VideoItem(
            id = -102L,
            title = "Screen Recording (HD)",
            displayName = "Screen_Recording_2026.mp4",
            contentUriString = "https://vjs.zencdn.net/v/oceans.mp4",
            duration = 46000L,
            size = 23014356L,
            width = 1280,
            height = 720,
            dateAdded = System.currentTimeMillis() / 1000 - 86400,
            folderName = "Record",
            path = "https://vjs.zencdn.net/v/oceans.mp4",
            mimeType = "video/mp4"
        ),
        VideoItem(
            id = -103L,
            title = "Restored Download Media",
            displayName = "restored_stream.mp4",
            contentUriString = "https://raw.githubusercontent.com/intel-iot-devkit/sample-videos/master/face-demographics-walking.mp4",
            duration = 20000L,
            size = 6406124L,
            width = 1920,
            height = 1080,
            dateAdded = System.currentTimeMillis() / 1000 - 172800,
            folderName = "Restored",
            path = "https://raw.githubusercontent.com/intel-iot-devkit/sample-videos/master/face-demographics-walking.mp4",
            mimeType = "video/mp4"
        ),
        VideoItem(
            id = -104L,
            title = "Sample Video Animation",
            displayName = "sample_flower.mp4",
            contentUriString = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
            duration = 5000L,
            size = 1128375L,
            width = 1280,
            height = 720,
            dateAdded = System.currentTimeMillis() / 1000 - 259200,
            folderName = "Sample",
            path = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
            mimeType = "video/mp4"
        ),
        VideoItem(
            id = -105L,
            title = "WhatsApp Shared Video Clip",
            displayName = "VID-WA0007.mp4",
            contentUriString = "https://vjs.zencdn.net/v/oceans.mp4?tag=whatsapp",
            duration = 46000L,
            size = 23014356L,
            width = 1280,
            height = 720,
            dateAdded = System.currentTimeMillis() / 1000 - 345600,
            folderName = "WhatsApp Videos",
            path = "https://vjs.zencdn.net/v/oceans.mp4?tag=whatsapp",
            mimeType = "video/mp4"
        ),
        VideoItem(
            id = -106L,
            title = "Hindi TV Shows & Highlights",
            displayName = "hindi_shows_highlight.mp4",
            contentUriString = "https://raw.githubusercontent.com/bower-media-samples/big-buck-bunny-1080p-30s/master/video.mp4?tag=shows",
            duration = 30000L,
            size = 22718509L,
            width = 1920,
            height = 1080,
            dateAdded = System.currentTimeMillis() / 1000 - 432000,
            folderName = "Best Hindi TV Shows",
            path = "https://raw.githubusercontent.com/bower-media-samples/big-buck-bunny-1080p-30s/master/video.mp4?tag=shows",
            mimeType = "video/mp4"
        )
    )

    fun fetchDeviceVideos(): List<VideoItem> {
        val videoList = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.MIME_TYPE,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            } else {
                MediaStore.Video.Media.DATA
            }
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val titleCol = c.getColumnIndex(MediaStore.Video.Media.TITLE)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dataCol = c.getColumnIndex(MediaStore.Video.Media.DATA)
                val mimeCol = c.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val bucketCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    c.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                } else -1

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val displayName = c.getString(nameCol) ?: "Video_$id"
                    val title = if (titleCol != -1) c.getString(titleCol) ?: displayName else displayName
                    val duration = c.getLong(durCol)
                    val size = c.getLong(sizeCol)
                    val width = c.getInt(widthCol)
                    val height = c.getInt(heightCol)
                    val dateAdded = c.getLong(dateCol)
                    val path = if (dataCol != -1) c.getString(dataCol) ?: "" else ""
                    val mimeType = if (mimeCol != -1) c.getString(mimeCol) ?: "video/*" else "video/*"

                    val folderName = if (bucketCol != -1 && bucketCol < c.columnCount) {
                        c.getString(bucketCol) ?: getFolderNameFromPath(path)
                    } else {
                        getFolderNameFromPath(path)
                    }

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    videoList.add(
                        VideoItem(
                            id = id,
                            title = title,
                            displayName = displayName,
                            contentUriString = contentUri,
                            duration = duration,
                            size = size,
                            width = width,
                            height = height,
                            dateAdded = dateAdded,
                            folderName = folderName,
                            path = path,
                            mimeType = mimeType
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return videoList
    }

    private fun getFolderNameFromPath(path: String): String {
        return try {
            if (path.isNotEmpty()) {
                val file = File(path)
                file.parentFile?.name ?: "Internal Storage"
            } else {
                "Internal Storage"
            }
        } catch (e: Exception) {
            "Internal Storage"
        }
    }

    fun observeAllVideosWithRecords(includeSamplesIfEmpty: Boolean = true): Flow<List<VideoItem>> {
        return combine(
            flow {
                val deviceVideos = fetchDeviceVideos()
                val list = if (deviceVideos.isEmpty() && includeSamplesIfEmpty) {
                    sampleVideos
                } else {
                    deviceVideos
                }
                emit(list)
            },
            videoDao.getAllRecordsFlow(),
            deletedUrisFlow,
            refreshTrigger
        ) { videos, records, deletedUris, _ ->
            val recordMap = records.associateBy { it.uri }
            videos
                .filterNot { it.contentUriString in deletedUris }
                .map { video ->
                    val record = recordMap[video.contentUriString]
                    if (record != null) {
                        video.copy(
                            playbackPosition = record.playbackPosition,
                            isFavorite = record.isFavorite,
                            lastPlayedTimestamp = record.lastPlayedTimestamp
                        )
                    } else {
                        video
                    }
                }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun savePlaybackPosition(video: VideoItem, positionMs: Long) {
        withContext(Dispatchers.IO) {
            val currentRecord = videoDao.getRecordByUri(video.contentUriString)
            val isFav = currentRecord?.isFavorite ?: video.isFavorite
            videoDao.insertOrUpdate(
                VideoEntity(
                    uri = video.contentUriString,
                    title = video.title,
                    playbackPosition = positionMs,
                    duration = video.duration,
                    isFavorite = isFav,
                    lastPlayedTimestamp = System.currentTimeMillis(),
                    folderName = video.folderName
                )
            )
        }
    }

    suspend fun toggleFavorite(video: VideoItem) {
        withContext(Dispatchers.IO) {
            val currentRecord = videoDao.getRecordByUri(video.contentUriString)
            val newFavStatus = !(currentRecord?.isFavorite ?: video.isFavorite)
            if (currentRecord != null) {
                videoDao.updateFavorite(video.contentUriString, newFavStatus)
            } else {
                videoDao.insertOrUpdate(
                    VideoEntity(
                        uri = video.contentUriString,
                        title = video.title,
                        playbackPosition = video.playbackPosition,
                        duration = video.duration,
                        isFavorite = newFavStatus,
                        lastPlayedTimestamp = video.lastPlayedTimestamp,
                        folderName = video.folderName
                    )
                )
            }
        }
    }

    suspend fun removeFromHistory(uri: String) {
        withContext(Dispatchers.IO) {
            videoDao.removeFromHistory(uri)
        }
    }

    fun buildFolderList(videos: List<VideoItem>): List<FolderItem> {
        val folderMap = mutableMapOf<String, MutableList<VideoItem>>()
        videos.forEach { v ->
            val folder = if (v.folderName.isBlank()) "Root" else v.folderName
            folderMap.getOrPut(folder) { mutableListOf() }.add(v)
        }
        return folderMap.map { (name, list) ->
            FolderItem(
                name = name,
                path = list.firstOrNull()?.path ?: "",
                videoCount = list.size,
                totalSize = list.sumOf { it.size },
                representativeUri = list.firstOrNull()?.contentUriString
            )
        }.sortedByDescending { it.videoCount }
    }

    fun getVideoMetadataFromUri(uri: Uri): VideoItem {
        var displayName = uri.lastPathSegment ?: "Selected Video"
        var size = 0L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return VideoItem(
            id = System.currentTimeMillis(),
            title = displayName.substringBeforeLast("."),
            displayName = displayName,
            contentUriString = uri.toString(),
            duration = 0L,
            size = size,
            width = 1920,
            height = 1080,
            dateAdded = System.currentTimeMillis() / 1000,
            folderName = "Storage Access",
            path = uri.toString(),
            mimeType = context.contentResolver.getType(uri) ?: "video/*"
        )
    }

    fun createDeleteIntentSender(video: VideoItem): IntentSender? {
        return try {
            val uri = Uri.parse(video.contentUriString)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && uri.authority == MediaStore.AUTHORITY) {
                MediaStore.createDeleteRequest(context.contentResolver, listOf(uri)).intentSender
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteVideo(video: VideoItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(video.contentUriString)
            var deleted = false

            // 1. If it's a file path or local app file, delete direct
            if (video.path.isNotBlank()) {
                val file = File(video.path)
                if (file.exists() && file.canWrite()) {
                    deleted = file.delete()
                }
            }

            // 2. Content Resolver deletion (Pre-Android 11 or own content)
            if (uri.scheme == "content") {
                try {
                    val count = context.contentResolver.delete(uri, null, null)
                    if (count > 0) deleted = true
                } catch (_: Exception) {}
            }

            // 3. Remove from Room DB
            videoDao.deleteRecord(video.contentUriString)

            // 4. Mark URI as deleted in reactive filter
            notifyVideoDeleted(video.contentUriString)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            notifyVideoDeleted(video.contentUriString)
            false
        }
    }

    fun notifyVideoDeleted(uriString: String) {
        val updated = deletedUrisFlow.value.toMutableSet()
        updated.add(uriString)
        deletedUrisFlow.value = updated
        requestRefresh()
    }

    fun requestRefresh() {
        refreshTrigger.value += 1
    }
}
