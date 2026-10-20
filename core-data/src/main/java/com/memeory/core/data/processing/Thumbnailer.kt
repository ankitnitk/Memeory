package com.memeory.core.data.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.roundToInt

/**
 * Makes the small preview images shown in the grid.
 *
 * Big or odd images (huge GIFs, very long screenshots) are shrunk while they
 * are being read, so the original is never fully loaded into memory. For GIFs
 * and animated WebP, the preview is the first frame.
 */
internal object Thumbnailer {

    /** Grid cells are square, so the shorter side matters most. */
    private const val SHORT_SIDE = 320
    private const val MAX_LONG_SIDE = 1280
    private const val MAX_DECODE_PIXELS = 8_000_000L

    data class Dimensions(val width: Int, val height: Int)

    /** [open] must return a fresh stream of the original each time it is called. */
    fun create(open: () -> InputStream, mimeType: String, target: File): Dimensions {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        val width = bounds.outWidth
        val height = bounds.outHeight
        require(width > 0 && height > 0) { "Not a readable image" }

        var sample = 1
        while (minOf(width, height) / (sample * 2) >= SHORT_SIDE) sample *= 2
        while ((width.toLong() / sample) * (height.toLong() / sample) > MAX_DECODE_PIXELS) sample *= 2

        val decoded = open().use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Image could not be decoded")

        val rotation = if (mimeType in EXIF_TYPES) readRotation(open) else 0
        val scaled = scaleDown(decoded)
        val finalBitmap = rotate(scaled, rotation)

        val temp = File(target.parentFile, target.name + ".tmp")
        try {
            FileOutputStream(temp).use { finalBitmap.compress(webpFormat(), 78, it) }
            check(temp.renameTo(target)) { "Could not save thumbnail" }
        } finally {
            temp.delete()
            listOf(decoded, scaled, finalBitmap).distinct().forEach { it.recycle() }
        }

        return if (rotation == 90 || rotation == 270) Dimensions(height, width) else Dimensions(width, height)
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val short = minOf(bitmap.width, bitmap.height)
        val long = maxOf(bitmap.width, bitmap.height)
        val factor = minOf(SHORT_SIDE.toFloat() / short, MAX_LONG_SIDE.toFloat() / long)
        if (factor >= 1f) return bitmap
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * factor).roundToInt().coerceAtLeast(1),
            (bitmap.height * factor).roundToInt().coerceAtLeast(1),
            true,
        )
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** Photos from cameras can be stored sideways with a "rotate me" note; respect it. */
    private fun readRotation(open: () -> InputStream): Int =
        runCatching { open().use { ExifInterface(it).rotationDegrees } }.getOrDefault(0)

    @Suppress("DEPRECATION")
    private fun webpFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY
        else Bitmap.CompressFormat.WEBP

    private val EXIF_TYPES = setOf("image/jpeg", "image/heif", "image/heic", "image/webp")
}
