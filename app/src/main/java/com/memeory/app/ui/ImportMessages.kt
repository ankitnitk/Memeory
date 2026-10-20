package com.memeory.app.ui

import android.content.res.Resources
import com.memeory.app.R
import com.memeory.core.data.importing.ImportResult

/** Turns an import result into a short sentence, e.g. "Saved 3 memes · 1 was already saved". */
object ImportMessages {
    fun describe(res: Resources, result: ImportResult): String {
        val parts = buildList {
            if (result.added > 0) {
                add(res.getQuantityString(R.plurals.import_added, result.added, result.added))
            }
            if (result.duplicates > 0) {
                add(res.getQuantityString(R.plurals.import_duplicates, result.duplicates, result.duplicates))
            }
            if (result.failed > 0) {
                add(res.getQuantityString(R.plurals.import_failed, result.failed, result.failed))
            }
        }
        return if (parts.isEmpty()) res.getString(R.string.import_nothing) else parts.joinToString(" · ")
    }
}
