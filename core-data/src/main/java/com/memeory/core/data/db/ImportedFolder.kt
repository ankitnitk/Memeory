package com.memeory.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A folder (e.g. WhatsApp Images) the user asked Memeory to watch. */
@Entity(
    tableName = "folders",
    indices = [Index(value = ["treeUri"], unique = true)],
)
data class ImportedFolder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Android's address for the folder; Memeory has lasting read-only access to it. */
    val treeUri: String,
    val name: String,
    val addedAt: Long,
    val lastScanAt: Long? = null,
)
