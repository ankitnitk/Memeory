package com.memeory.core.data.importing

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.util.Log
import com.memeory.core.data.db.ImportedFolder
import com.memeory.core.data.db.Meme
import com.memeory.core.data.db.MemeoryDatabase
import com.memeory.core.data.db.StorageKind
import com.memeory.core.data.work.MemeWork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles folders the user added (e.g. WhatsApp Images). Memes in these
 * folders are NOT copied. Memeory remembers where each one is, and checks the
 * folder again each time the app opens, picking up new memes and noticing
 * deleted ones.
 */
class FolderScanner(
    private val context: Context,
    private val db: MemeoryDatabase,
) {
    private data class FoundImage(val uri: String, val mimeType: String, val size: Long, val modified: Long)

    /** Returns true if the folder is new, false if it was already added. */
    suspend fun addFolder(treeUri: Uri): Boolean = withContext(Dispatchers.IO) {
        // Keep read-only access after the phone restarts.
        context.contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val name = folderName(treeUri) ?: treeUri.lastPathSegment ?: "Folder"
        val id = db.folderDao().insert(
            ImportedFolder(treeUri = treeUri.toString(), name = name, addedAt = System.currentTimeMillis())
        )
        MemeWork.enqueueFolderScan(context, restartIfRunning = true)
        id != -1L
    }

    /** Checks every added folder. Returns how many new memes were found. */
    suspend fun scanAll(isStopped: () -> Boolean): Int = withContext(Dispatchers.IO) {
        var added = 0
        for (folder in db.folderDao().all()) {
            if (isStopped()) break
            added += scan(folder, isStopped)
        }
        if (added > 0) MemeWork.enqueueProcessing(context)
        added
    }

    private suspend fun scan(folder: ImportedFolder, isStopped: () -> Boolean): Int {
        val tree = Uri.parse(folder.treeUri)
        val found = try {
            listImages(tree, isStopped)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Access was removed, or the folder no longer exists. Leave the
            // memes as they are rather than marking everything missing.
            Log.w(TAG, "Could not read folder ${folder.name}", e)
            return 0
        }
        // A half-finished scan would wrongly mark memes as missing.
        if (isStopped()) return 0

        val dao = db.memeDao()
        val known = dao.referencesInFolder(folder.id).associate { it.sourceUri to it.isMissing }
        val foundUris = found.mapTo(HashSet()) { it.uri }
        val now = System.currentTimeMillis()

        val newMemes = found
            .filter { it.uri !in known }
            .map {
                Meme(
                    storage = StorageKind.REFERENCED,
                    fileName = null,
                    sourceUri = it.uri,
                    folderId = folder.id,
                    mimeType = it.mimeType,
                    sizeBytes = it.size,
                    contentHash = null,
                    // Use the file's own date so folder memes sort like a gallery.
                    addedAt = if (it.modified > 0) it.modified else now,
                )
            }
        newMemes.chunked(CHUNK).forEach { dao.insertAll(it) }

        val goneNow = known.filter { (uri, missing) -> !missing && uri !in foundUris }.keys.toList()
        val backAgain = known.filter { (uri, missing) -> missing && uri in foundUris }.keys.toList()
        goneNow.chunked(CHUNK).forEach { dao.setMissing(it, true) }
        backAgain.chunked(CHUNK).forEach { dao.setMissing(it, false) }

        db.folderDao().markScanned(folder.id, now)
        return newMemes.size
    }

    /** Walks the folder (and its sub-folders) and lists every image in it. */
    private fun listImages(tree: Uri, isStopped: () -> Boolean): List<FoundImage> {
        val result = mutableListOf<FoundImage>()
        val toVisit = ArrayDeque<Pair<String, Int>>()
        toVisit.add(DocumentsContract.getTreeDocumentId(tree) to 0)
        val projection = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_SIZE,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_DISPLAY_NAME,
        )

        while (toVisit.isNotEmpty() && !isStopped()) {
            val (documentId, depth) = toVisit.removeFirst()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, documentId)
            val cursor = context.contentResolver.query(children, projection, null, null, null)
                ?: if (depth == 0) throw IOException("Folder could not be read") else continue

            cursor.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val mimeType = c.getString(1) ?: continue
                    val name = c.getString(4).orEmpty()
                    if (name.startsWith(".")) continue // hidden files and folders

                    if (mimeType == Document.MIME_TYPE_DIR) {
                        if (depth < MAX_DEPTH) toVisit.add(id to depth + 1)
                    } else if (mimeType.startsWith("image/")) {
                        result += FoundImage(
                            uri = DocumentsContract.buildDocumentUriUsingTree(tree, id).toString(),
                            mimeType = mimeType,
                            size = if (c.isNull(2)) 0 else c.getLong(2),
                            modified = if (c.isNull(3)) 0 else c.getLong(3),
                        )
                    }
                }
            }
        }
        return result
    }

    private fun folderName(tree: Uri): String? = try {
        val doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        context.contentResolver.query(doc, arrayOf(Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val TAG = "FolderScanner"
        const val CHUNK = 500
        const val MAX_DEPTH = 4
    }
}
