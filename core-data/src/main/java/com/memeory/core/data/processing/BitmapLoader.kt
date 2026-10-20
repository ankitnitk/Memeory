package com.memeory.core.data.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Loads an image at a limited size, safely.
 *
 * Big or odd images (huge GIFs, very long screenshots) are shrunk while they
 * are being read, so the full original is never loaded into memory. For GIFs
 * and animated WebP this gives the first frame. Camera photos stored sideways
 * are turned the right way up.
 */
internal object BitmapLoader {

    /** [width] and [height] are the original image's size (after turning upright). */
    class Loaded(val bitmap: Bitmap, val width: Int, val height: Int)

    /** [open] must return a fresh stream of the original each time it is called. */
    fun load(
        open: () -> InputStream,
        mimeType: String,
        maxShortSide: Int,
        maxLongSide: Int,
        maxPixels: Long,
    ): Loaded {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        val width = bounds.outWidth
        val height = bounds.outHeight
        require(width > 0 && height > 0) { "Not a readable image" }

        // How much to shrink: just enough to respect every limit, never enlarge.
        val factor = minOf(
            1f,
            maxShortSide.toFloat() / minOf(width, height),
            maxLongSide.toFloat() / maxOf(width, height),
            sqrt(maxPixels.toFloat() / (width.toFloat() * height)),
        )

        // Fast rough shrink while reading (halving steps), then an exact resize.
        var sample = 1
        while (factor * sample * 2 <= 1f) sample *= 2
        val decoded = open().use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Image could not be decoded")

        val targetWidth = (width * factor).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * factor).roundToInt().coerceAtLeast(1)
        val scaled = if (decoded.width > targetWidth) {
            Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true).also { decoded.recycle() }
        } else {
            decoded
        }

        val rotation = if (mimeType in EXIF_TYPES) readRotation(open) else 0
        if (rotation == 0) return Loaded(scaled, width, height)

        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val upright = Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
        if (upright !== scaled) scaled.recycle()
        val sideways = rotation == 90 || rotation == 270
        return Loaded(upright, if (sideways) height else width, if (sideways) width else height)
    }

    private fun readRotation(open: () -> InputStream): Int =
        runCatching { open().use { ExifInterface(it).rotationDegrees } }.getOrDefault(0)

    private val EXIF_TYPES = setOf("image/jpeg", "image/heif", "image/heic", "image/webp")
}
