package com.memeory.core.data.importing

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import com.memeory.core.data.db.Meme
import com.memeory.core.data.db.MemeoryDatabase
import com.memeory.core.data.db.StorageKind
import com.memeory.core.data.files.ImageTypes
import com.memeory.core.data.files.MemeFiles
import com.memeory.core.data.work.MemeWork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/** What happened when the user saved some memes. */
data class ImportResult(
    val added: Int,
    val duplicates: Int,
    val failed: Int,
)

/**
 * Copies memes into Memeory's private storage. Used for images shared into
 * the app and images picked from the gallery, because Android only lends
 * those to us briefly.
 */
class MemeImporter(
    private val context: Context,
    private val db: MemeoryDatabase,
    private val files: MemeFiles,
) {
    private enum class Outcome { ADDED, DUPLICATE, FAILED }

    suspend fun importCopies(uris: List<Uri>): ImportResult = withContext(Dispatchers.IO) {
        var added = 0
        var duplicates = 0
        var failed = 0
        for (uri in uris.distinct()) {
            when (copyOne(uri)) {
                Outcome.ADDED -> added++
                Outcome.DUPLICATE -> duplicates++
                Outcome.FAILED -> failed++
            }
        }
        // Thumbnails are made in the background so saving feels instant.
        if (added > 0) MemeWork.enqueueProcessing(context)
        ImportResult(added, duplicates, failed)
    }

    private suspend fun copyOne(uri: Uri): Outcome {
        // Only accept content:// addresses. A file:// address could point at
        // Memeory's own private files.
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return Outcome.FAILED

        val temp = files.newIncomingFile()
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val header = ByteArray(ImageTypes.HEADER_SIZE)
            var headerLength = 0
            var size = 0L

            val input = context.contentResolver.openInputStream(uri) ?: return Outcome.FAILED
            input.use { stream ->
                FileOutputStream(temp).use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        if (headerLength < header.size) {
                            val take = minOf(read, header.size - headerLength)
                            System.arraycopy(buffer, 0, header, headerLength, take)
                            headerLength += take
                        }
                        size += read
                        if (size > MAX_BYTES) return Outcome.FAILED
                        digest.update(buffer, 0, read)
                        out.write(buffer, 0, read)
                    }
                }
            }

            val mimeType = ImageTypes.sniff(header, headerLength) ?: return Outcome.FAILED
            val hash = digest.digest().toHex()
            if (db.memeDao().hashExists(hash)) return Outcome.DUPLICATE

            val fileName = "${UUID.randomUUID()}.${ImageTypes.extensionFor(mimeType)}"
            val target = files.memeFile(fileName)
            if (!temp.renameTo(target)) return Outcome.FAILED

            val id = db.memeDao().insert(
                Meme(
                    storage = StorageKind.COPIED,
                    fileName = fileName,
                    sourceUri = null,
                    folderId = null,
                    mimeType = mimeType,
                    sizeBytes = size,
                    contentHash = hash,
                    addedAt = System.currentTimeMillis(),
                )
            )
            if (id == -1L) {
                target.delete()
                return Outcome.FAILED
            }
            return Outcome.ADDED
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not save a meme", e)
            return Outcome.FAILED
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    private fun ByteArray.toHex(): String {
        val chars = CharArray(size * 2)
        forEachIndexed { i, b ->
            val v = b.toInt() and 0xFF
            chars[i * 2] = HEX[v ushr 4]
            chars[i * 2 + 1] = HEX[v and 0x0F]
        }
        return String(chars)
    }

    private companion object {
        const val TAG = "MemeImporter"
        /** Anything bigger is almost certainly not a meme (and could fill the phone). */
        const val MAX_BYTES = 50L * 1024 * 1024
        val HEX = "0123456789abcdef".toCharArray()
    }
}
