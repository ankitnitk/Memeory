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
import kotlin.coroutines.cancellation.CancellationException

/**
 * The background steps each new meme goes through:
 *  1. make a small preview (thumbnail)  -> Stage.THUMBNAIL_READY
 *  2. read the text printed on it       -> Stage.DONE
 */
class MemeProcessor(
    private val context: Context,
    private val db: MemeoryDatabase,
    private val files: MemeFiles,
) {
    private val textReader = TextReader()

    suspend fun makeThumbnail(meme: Meme) = withContext(Dispatchers.IO) {
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

    suspend fun readText(meme: Meme) = withContext(Dispatchers.IO) {
        val dao = db.memeDao()
        try {
            val text = textReader.read({ openOriginal(meme) }, meme.mimeType)
            dao.setText(meme.id, text, Stage.DONE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FileNotFoundException) {
            // The original was deleted. Keep the meme (its preview still shows),
            // mark it missing, and don't try again.
            dao.setText(meme.id, null, Stage.DONE)
            dao.markFailed(meme.id, Stage.DONE, missing = true)
        } catch (e: Throwable) {
            // Unreadable image, or out of memory on a giant one. Mark it done
            // with no text so it isn't retried forever.
            Log.w(TAG, "Could not read text in meme ${meme.id}", e)
            dao.setText(meme.id, null, Stage.DONE)
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
