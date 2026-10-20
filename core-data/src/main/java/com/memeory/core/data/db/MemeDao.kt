package com.memeory.core.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** How many memes are still waiting for each background step. */
data class PendingCounts(
    val thumbnails: Int,
    val text: Int,
)

/** Everything the detail screen shows about one meme. */
data class MemeDetail(
    @Embedded val meme: Meme,
    val folderName: String?,
)

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

    /**
     * Search. [match] comes from SearchQuery.toFtsMatch (e.g. "mond* blu*").
     * Favourites and often-shared memes come first (they start counting in Phase 3).
     */
    @Query(
        """
        SELECT m.id, m.stage, m.isMissing, m.mimeType
        FROM memes AS m
        JOIN meme_fts ON m.id = meme_fts.rowid
        WHERE meme_fts MATCH :match AND m.stage >= 0
        ORDER BY m.isFavorite DESC, m.shareCount DESC, m.addedAt DESC, m.id DESC
        LIMIT 500
        """
    )
    fun search(match: String): Flow<List<GridMeme>>

    @Query(
        """
        SELECT m.*, f.name AS folderName
        FROM memes AS m LEFT JOIN folders AS f ON m.folderId = f.id
        WHERE m.id = :id
        """
    )
    fun observeDetail(id: Long): Flow<MemeDetail?>

    @Query(
        """
        SELECT COALESCE(SUM(stage = 0), 0) AS thumbnails,
               COALESCE(SUM(stage = 1), 0) AS text
        FROM memes
        """
    )
    fun observePendingCounts(): Flow<PendingCounts>

    @Query("SELECT * FROM memes WHERE stage = :stage ORDER BY id LIMIT :limit")
    suspend fun pendingAt(stage: Int, limit: Int): List<Meme>

    @Query("UPDATE memes SET stage = :stage, width = :width, height = :height WHERE id = :id")
    suspend fun markProcessed(id: Long, stage: Int, width: Int, height: Int)

    @Query("UPDATE memes SET ocrText = :text, stage = :stage WHERE id = :id")
    suspend fun setText(id: Long, text: String?, stage: Int)

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
