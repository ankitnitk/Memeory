package com.memeory.core.data.db

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * The search index: a fast word lookup over the text read from each meme and
 * the user's note. Room keeps it in sync with the memes table automatically.
 * Tags are added to it in Phase 3.
 *
 * "unicode61" splits words correctly for most languages (including Hindi
 * later) and ignores upper/lower case and accents.
 */
@Entity(tableName = "meme_fts")
@Fts4(contentEntity = Meme::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
data class MemeFts(
    val ocrText: String?,
    val note: String?,
)
