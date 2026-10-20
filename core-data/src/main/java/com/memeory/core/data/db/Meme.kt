package com.memeory.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Where the meme's image lives. */
enum class StorageKind {
    /** Memeory keeps its own copy (shared into the app, or picked from the gallery). */
    COPIED,

    /** The image stays in a folder the user added; Memeory only remembers where it is. */
    REFERENCED,
}

/** How far a meme has got through background processing. */
object Stage {
    /** Could not be read (e.g. a damaged file). Hidden from the grid. */
    const val FAILED = -1

    /** Just added; the thumbnail has not been made yet. */
    const val NEW = 0

    /** Thumbnail is ready. Phase 2 adds text reading after this step. */
    const val THUMBNAIL_READY = 1

    /** Everything is done. */
    const val DONE = 2
}

@Entity(
    tableName = "memes",
    foreignKeys = [
        ForeignKey(
            entity = ImportedFolder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            // Removing a folder from Memeory removes its memes from Memeory too
            // (the files in the folder itself are never touched).
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("contentHash"),
        Index(value = ["sourceUri"], unique = true),
        Index("folderId"),
        Index("stage"),
        Index("addedAt"),
    ],
)
data class Meme(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val storage: StorageKind,
    /** For COPIED memes: the file name inside Memeory's private "memes" folder. */
    val fileName: String?,
    /** For REFERENCED memes: the Android address of the original file. */
    val sourceUri: String?,
    /** For REFERENCED memes: which added folder it came from. */
    val folderId: Long?,
    val mimeType: String,
    val sizeBytes: Long,
    /** Fingerprint of a copied file, used to avoid saving the same meme twice. */
    val contentHash: String?,
    val width: Int = 0,
    val height: Int = 0,
    val stage: Int = Stage.NEW,
    /** True when a REFERENCED meme's original file was deleted or moved. */
    val isMissing: Boolean = false,
    val addedAt: Long,
    // The fields below are filled in by later phases. They exist now so the
    // database layout does not have to change for them.
    val ocrText: String? = null,
    val note: String? = null,
    val isFavorite: Boolean = false,
    val shareCount: Int = 0,
    val lastSharedAt: Long? = null,
)

/** The few fields the grid needs, so scrolling thousands of memes stays light. */
data class GridMeme(
    val id: Long,
    val stage: Int,
    val isMissing: Boolean,
    val mimeType: String,
)

/** A referenced meme's address and whether it is currently missing. */
data class FolderRef(
    val sourceUri: String,
    val isMissing: Boolean,
)
