package com.memeory.core.data.search

/**
 * Turns what the user typed into a search the database understands.
 *
 * "Mond  blues!" becomes "mond* blues*": every word must appear, and each word
 * also matches longer words that start with it, so "mond" finds "Monday".
 * Punctuation and symbols are dropped, so typing can never break the search.
 */
object SearchQuery {
    private const val MAX_WORDS = 8

    // Splits on anything that isn't a letter, digit or accent mark
    // (accent marks keep Hindi and other scripts' words whole).
    private val separators = Regex("[^\\p{L}\\p{N}\\p{M}]+")

    /** Returns null when there is nothing to search for. */
    fun toFtsMatch(input: String): String? {
        val words = input.lowercase()
            .split(separators)
            .filter { it.isNotEmpty() }
            .take(MAX_WORDS)
        if (words.isEmpty()) return null
        return words.joinToString(" ") { "$it*" }
    }
}
