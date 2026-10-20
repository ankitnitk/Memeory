package com.memeory.core.data.files

import android.content.Context
import java.io.File
import java.util.UUID

/**
 * Where Memeory keeps files, all inside the app's private storage
 * (other apps cannot read it):
 *  - memes/     copies of shared or gallery-picked memes
 *  - thumbs/    small previews for the grid (made for every meme, including
 *               folder ones, so the grid stays fast)
 *  - incoming/  half-finished copies; cleaned up automatically
 */
class MemeFiles(context: Context) {
    private val root = context.filesDir

    val memesDir = File(root, "memes")
    val thumbsDir = File(root, "thumbs")
    private val incomingDir = File(root, "incoming")

    init {
        memesDir.mkdirs()
        thumbsDir.mkdirs()
        incomingDir.mkdirs()
    }

    fun memeFile(fileName: String) = File(memesDir, fileName)

    fun thumbnailFile(memeId: Long) = File(thumbsDir, "$memeId.webp")

    fun newIncomingFile() = File(incomingDir, "${UUID.randomUUID()}.part")

    /** Removes copies left behind if the app was closed mid-import. */
    fun cleanStaleIncoming(olderThanMs: Long = 60 * 60 * 1000L) {
        val now = System.currentTimeMillis()
        incomingDir.listFiles()
            ?.filter { now - it.lastModified() > olderThanMs }
            ?.forEach { it.delete() }
    }
}
