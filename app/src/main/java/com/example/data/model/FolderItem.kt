package com.example.data.model

import java.util.Locale

data class FolderItem(
    val name: String,
    val path: String,
    val videoCount: Int,
    val totalSize: Long,
    val representativeUri: String? = null
) {
    val formattedTotalSize: String
        get() {
            if (totalSize <= 0) return "0 MB"
            val mb = totalSize / (1024.0 * 1024.0)
            val gb = mb / 1024.0
            return if (gb >= 1.0) {
                String.format(Locale.US, "%.1f GB", gb)
            } else {
                String.format(Locale.US, "%.0f MB", mb)
            }
        }
}
