package com.memeory.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemeDao {

    /** Returns the new id, or -1 if an identical row already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(meme: Meme): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(memes: List<Meme>): List<Long>

    /** Newest first. Updates automatically whenever memes change. */
    @Query("SELECT id, stage, isMissing, mimeType FROM memes WHERE stage >= 0 ORDER BY addedAt DESC, id DESC")
    fun observeGrid(): Flow<List<GridMeme>>

    @Query("SELECT COUNT(*) FROM memes WHERE stage = 0")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM memes WHERE stage = 0 ORDER BY id LIMIT :limit")
    suspend fun pending(limit: Int): List<Meme>

    @Query("UPDATE memes SET stage = :stage, width = :width, height = :height WHERE id = :id")
    suspend fun markProcessed(id: Long, stage: Int, width: Int, height: Int)

    @Query("UPDATE memes SET stage = :stage, isMissing = :missing WHERE id = :id")
    suspend fun markFailed(id: Long, stage: Int, missing: Boolean)

    @Query("DELETE FROM memes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM memes WHERE contentHash = :hash)")
    suspend fun hashExists(hash: String): Boolean

    @Query("SELECT sourceUri, isMissing FROM memes WHERE folderId = :folderId AND sourceUri IS NOT NULL")
    suspend fun referencesInFolder(folderId: Long): List<FolderRef>

    /** Call with at most ~500 addresses at a time (older Android limits query size). */
    @Query("UPDATE memes SET isMissing = :missing WHERE sourceUri IN (:uris)")
    suspend fun setMissing(uris: List<String>, missing: Boolean)
}
