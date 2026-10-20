package com.memeory.core.data

import android.content.Context
import com.memeory.core.data.db.FolderDao
import com.memeory.core.data.db.MemeDao
import com.memeory.core.data.db.MemeoryDatabase
import com.memeory.core.data.files.MemeFiles
import com.memeory.core.data.importing.FolderScanner
import com.memeory.core.data.importing.MemeImporter
import com.memeory.core.data.processing.MemeProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The one place the app (and later the meme keyboard) gets everything from:
 * database, files, importing and processing. Created once, on first use.
 */
class MemeoryCore private constructor(context: Context) {

    val database: MemeoryDatabase = MemeoryDatabase.build(context)
    val files = MemeFiles(context)

    /** For work that must finish even if the screen that started it closes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val importer = MemeImporter(context, database, files)
    val folderScanner = FolderScanner(context, database)
    val processor = MemeProcessor(context, database, files)

    val memes: MemeDao get() = database.memeDao()
    val folders: FolderDao get() = database.folderDao()

    init {
        appScope.launch { files.cleanStaleIncoming() }
    }

    companion object {
        @Volatile private var instance: MemeoryCore? = null

        fun get(context: Context): MemeoryCore =
            instance ?: synchronized(this) {
                instance ?: MemeoryCore(context.applicationContext).also { instance = it }
            }
    }
}
