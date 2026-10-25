package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_records")
data class VideoEntity(
    @PrimaryKey
    val uri: String,
    val title: String,
    val playbackPosition: Long = 0L,
    val duration: Long = 0L,
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long = 0L,
    val folderName: String = ""
)
