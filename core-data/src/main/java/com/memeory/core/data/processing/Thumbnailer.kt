package com.memeory.core.data.processing

import android.graphics.Bitmap
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/** Makes the small preview images shown in the grid. */
internal object Thumbnailer {

    data class Dimensions(val width: Int, val height: Int)

    /** Saves a preview to [target] and returns the original image's size. */
    fun create(open: () -> InputStream, mimeType: String, target: File): Dimensions {
        // Grid cells are square: ~320px on the short side is sharp on any phone.
        val loaded = BitmapLoader.load(
            open, mimeType,
            maxShortSide = 320,
            maxLongSide = 1280,
            maxPixels = 600_000,
        )
        val temp = File(target.parentFile, target.name + ".tmp")
        try {
            FileOutputStream(temp).use { loaded.bitmap.compress(webpFormat(), 78, it) }
            check(temp.renameTo(target)) { "Could not save thumbnail" }
        } finally {
            temp.delete()
            loaded.bitmap.recycle()
        }
        return Dimensions(loaded.width, loaded.height)
    }

    @Suppress("DEPRECATION")
    private fun webpFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY
        else Bitmap.CompressFormat.WEBP
}
