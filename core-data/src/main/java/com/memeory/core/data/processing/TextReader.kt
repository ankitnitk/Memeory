package com.memeory.core.data.processing

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Reads the words printed on a meme, on the phone, with Google ML Kit.
 * Handles Latin-script languages (English, Swahili, Sheng, Hinglish typed in
 * English letters...). Hindi in Devanagari script is planned for later.
 */
internal class TextReader {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun read(open: () -> InputStream, mimeType: String): String {
        // Big enough for small captions to stay readable, small enough to be quick.
        val loaded = BitmapLoader.load(
            open, mimeType,
            maxShortSide = 1600,
            maxLongSide = 2400,
            maxPixels = 4_000_000,
        )
        try {
            val result = recognizer.process(InputImage.fromBitmap(loaded.bitmap, 0)).await()
            return tidy(result)
        } finally {
            loaded.bitmap.recycle()
        }
    }

    /** One line per text block, with stray spaces and line breaks squeezed out. */
    private fun tidy(text: Text): String =
        text.textBlocks
            .map { it.text.replace(WHITESPACE, " ").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
