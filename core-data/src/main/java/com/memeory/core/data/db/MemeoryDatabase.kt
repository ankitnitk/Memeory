package com.memeory.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Version history:
 *  1 - Phase 1 test build: memes and folders.
 *  2 - Phase 2: adds the search index (meme_fts).
 *
 * Version 1 only ever existed on test phones, so it is simply cleared rather
 * than upgraded. From version 2 on, every change gets a proper upgrade step
 * so nobody loses memes.
 */
@Database(
    entities = [Meme::class, ImportedFolder::class, MemeFts::class],
    version = 2,
    exportSchema = true,
)
abstract class MemeoryDatabase : RoomDatabase() {
    abstract fun memeDao(): MemeDao
    abstract fun folderDao(): FolderDao

    companion object {
        fun build(context: Context): MemeoryDatabase =
            Room.databaseBuilder(context, MemeoryDatabase::class.java, "memeory.db")
                .fallbackToDestructiveMigrationFrom(1)
                .build()
    }
}
