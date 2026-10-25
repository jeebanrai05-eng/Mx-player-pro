package com.example.data.model

import java.util.Locale
import java.util.concurrent.TimeUnit

data class VideoItem(
    val id: Long,
    val title: String,
    val displayName: String,
    val contentUriString: String,
    val duration: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val dateAdded: Long,
    val folderName: String,
    val path: String,
    val mimeType: String,
    val playbackPosition: Long = 0L,
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long = 0L
) {
    val formattedDuration: String
        get() {
            if (duration <= 0) return "--:--"
            val hours = TimeUnit.MILLISECONDS.toHours(duration)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60
            return if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            if (size <= 0) return "0 MB"
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                else -> String.format(Locale.US, "%.0f KB", kb)
            }
        }

    val resolutionLabel: String
        get() {
            val maxDim = maxOf(width, height)
            return when {
                maxDim >= 3840 -> "4K"
                maxDim >= 2560 -> "2K"
                maxDim >= 1920 -> "1080p"
                maxDim >= 1280 -> "720p"
                maxDim >= 854 -> "480p"
                maxDim > 0 -> "${maxDim}p"
                else -> "HD"
            }
        }

    val watchProgressFraction: Float
        get() {
            return if (duration > 0 && playbackPosition > 0) {
                (playbackPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else 0f
        }
}
