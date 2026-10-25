package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Query("SELECT * FROM video_records")
    fun getAllRecordsFlow(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM video_records WHERE uri = :uri LIMIT 1")
    suspend fun getRecordByUri(uri: String): VideoEntity?

    @Query("SELECT * FROM video_records WHERE isFavorite = 1")
    fun getFavoritesFlow(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM video_records WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT 50")
    fun getRecentlyPlayedFlow(): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: VideoEntity)

    @Query("UPDATE video_records SET playbackPosition = :position, lastPlayedTimestamp = :timestamp WHERE uri = :uri")
    suspend fun updatePlaybackPosition(uri: String, position: Long, timestamp: Long)

    @Query("UPDATE video_records SET isFavorite = :isFavorite WHERE uri = :uri")
    suspend fun updateFavorite(uri: String, isFavorite: Boolean)

    @Query("DELETE FROM video_records WHERE uri = :uri")
    suspend fun deleteRecord(uri: String)

    @Query("UPDATE video_records SET lastPlayedTimestamp = 0 WHERE uri = :uri")
    suspend fun removeFromHistory(uri: String)
}
