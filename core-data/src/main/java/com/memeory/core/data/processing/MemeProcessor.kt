package com.memeory.core.data.processing

import android.content.Context
import android.net.Uri
import android.util.Log
import com.memeory.core.data.db.Meme
import com.memeory.core.data.db.MemeoryDatabase
import com.memeory.core.data.db.Stage
import com.memeory.core.data.db.StorageKind
import com.memeory.core.data.files.MemeFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * The background steps each new meme goes through.
 * Phase 1: make a thumbnail. Phase 2 adds reading the text in the image.
 */
class MemeProcessor(
    private val context: Context,
    private val db: MemeoryDatabase,
    private val files: MemeFiles,
) {
    suspend fun process(meme: Meme) = withContext(Dispatchers.IO) {
        val dao = db.memeDao()
        val result = runCatching {
            Thumbnailer.create({ openOriginal(meme) }, meme.mimeType, files.thumbnailFile(meme.id))
        }
        val size = result.getOrNull()

        when {
            size != null ->
                dao.markProcessed(meme.id, Stage.THUMBNAIL_READY, size.width, size.height)

            // A copied file we cannot read is useless, so remove it.
            meme.storage == StorageKind.COPIED -> {
                Log.w(TAG, "Removing unreadable meme ${meme.id}", result.exceptionOrNull())
                meme.fileName?.let { files.memeFile(it).delete() }
                dao.delete(meme.id)
            }

            // A folder meme stays in the database (hidden) so the next folder
            // check doesn't add it again.
            else -> {
                Log.w(TAG, "Could not read folder meme ${meme.id}", result.exceptionOrNull())
                dao.markFailed(meme.id, Stage.FAILED, missing = result.exceptionOrNull() is FileNotFoundException)
            }
        }
    }

    /** Opens the full-size original, wherever it lives. */
    fun openOriginal(meme: Meme): InputStream = when (meme.storage) {
        StorageKind.COPIED -> FileInputStream(files.memeFile(requireNotNull(meme.fileName)))
        StorageKind.REFERENCED ->
            context.contentResolver.openInputStream(Uri.parse(requireNotNull(meme.sourceUri)))
                ?: throw FileNotFoundException("Original is gone")
    }

    private companion object {
        const val TAG = "MemeProcessor"
    }
}
