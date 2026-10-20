package com.memeory.core.data.files

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageTypesTest {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }
    private fun ascii(text: String) = text.toByteArray(Charsets.US_ASCII)

    @Test fun jpeg() = assertEquals("image/jpeg", ImageTypes.sniff(bytes(0xFF, 0xD8, 0xFF, 0xE0)))

    @Test fun png() = assertEquals("image/png", ImageTypes.sniff(bytes(0x89, 'P'.code, 'N'.code, 'G'.code)))

    @Test fun gif() = assertEquals("image/gif", ImageTypes.sniff(ascii("GIF89a")))

    @Test fun webp() = assertEquals("image/webp", ImageTypes.sniff(ascii("RIFF\u0000\u0000\u0000\u0000WEBPVP8 ")))

    @Test fun heic() = assertEquals("image/heif", ImageTypes.sniff(ascii("\u0000\u0000\u0000\u0018ftypheic")))

    @Test fun avif() = assertEquals("image/avif", ImageTypes.sniff(ascii("\u0000\u0000\u0000\u001cftypavif")))

    @Test fun textFileIsNotAnImage() = assertNull(ImageTypes.sniff(ascii("hello world")))

    @Test fun shortHeaderIsSafe() = assertNull(ImageTypes.sniff(bytes(0xFF), length = 1))

    @Test fun lengthLimitIsRespected() =
        assertNull(ImageTypes.sniff(ascii("GIF89a"), length = 3))

    @Test fun extensions() {
        assertEquals("jpg", ImageTypes.extensionFor("image/jpeg"))
        assertEquals("gif", ImageTypes.extensionFor("image/gif"))
        assertEquals("img", ImageTypes.extensionFor("image/x-unknown"))
    }
}
