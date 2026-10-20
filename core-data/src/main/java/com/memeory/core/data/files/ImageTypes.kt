package com.memeory.core.data.files

/**
 * Works out what kind of image a file is by looking at its first few bytes,
 * instead of trusting the name or what the sending app claims.
 */
object ImageTypes {
    const val HEADER_SIZE = 16

    fun sniff(header: ByteArray, length: Int = header.size): String? {
        fun byteAt(i: Int) = if (i < length && i < header.size) header[i].toInt() and 0xFF else -1
        fun textAt(at: Int, text: String) = text.indices.all { byteAt(at + it) == text[it].code }

        return when {
            byteAt(0) == 0xFF && byteAt(1) == 0xD8 && byteAt(2) == 0xFF -> "image/jpeg"
            byteAt(0) == 0x89 && textAt(1, "PNG") -> "image/png"
            textAt(0, "GIF8") -> "image/gif"
            textAt(0, "RIFF") && textAt(8, "WEBP") -> "image/webp"
            textAt(4, "ftypavif") -> "image/avif"
            textAt(4, "ftyp") && HEIF_BRANDS.any { textAt(8, it) } -> "image/heif"
            textAt(0, "BM") -> "image/bmp"
            else -> null
        }
    }

    fun extensionFor(mimeType: String): String = when (mimeType) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        "image/avif" -> "avif"
        "image/heif", "image/heic" -> "heic"
        "image/bmp" -> "bmp"
        else -> "img"
    }

    private val HEIF_BRANDS = listOf("heic", "heix", "heif", "mif1", "msf1")
}
