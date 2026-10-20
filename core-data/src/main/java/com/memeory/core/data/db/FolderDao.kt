package com.memeory.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    /** Returns the new id, or -1 if this folder was already added. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(folder: ImportedFolder): Long

    @Query("SELECT * FROM folders ORDER BY name")
    suspend fun all(): List<ImportedFolder>

    @Query("SELECT * FROM folders ORDER BY name")
    fun observeAll(): Flow<List<ImportedFolder>>

    @Query("UPDATE folders SET lastScanAt = :time WHERE id = :id")
    suspend fun markScanned(id: Long, time: Long)
}
