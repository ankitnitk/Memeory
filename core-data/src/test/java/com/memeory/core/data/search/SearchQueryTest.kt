package com.memeory.core.data.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchQueryTest {

    @Test fun singleWordBecomesPrefix() = assertEquals("mond*", SearchQuery.toFtsMatch("mond"))

    @Test fun caseIsIgnored() = assertEquals("monday*", SearchQuery.toFtsMatch("MONDAY"))

    @Test fun multipleWordsAllRequired() =
        assertEquals("when* late*", SearchQuery.toFtsMatch("  when   late "))

    @Test fun punctuationAndSearchSyntaxAreDropped() =
        assertEquals("don* t* work* or*", SearchQuery.toFtsMatch("\"don't\" -work* (OR)"))

    @Test fun orIsJustAWordNotAnOperator() =
        assertEquals("a* or* b*", SearchQuery.toFtsMatch("a OR b"))

    @Test fun hindiWordsStayWhole() = assertEquals("नमस्ते*", SearchQuery.toFtsMatch("नमस्ते!"))

    @Test fun blankGivesNull() {
        assertNull(SearchQuery.toFtsMatch(""))
        assertNull(SearchQuery.toFtsMatch("   "))
        assertNull(SearchQuery.toFtsMatch("?!*\""))
    }

    @Test fun wordCountIsLimited() =
        assertEquals(8, SearchQuery.toFtsMatch("a b c d e f g h i j")!!.split(" ").size)
}
