package com.memeory.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Meme::class, ImportedFolder::class],
    version = 1,
    exportSchema = true,
)
abstract class MemeoryDatabase : RoomDatabase() {
    abstract fun memeDao(): MemeDao
    abstract fun folderDao(): FolderDao

    companion object {
        fun build(context: Context): MemeoryDatabase =
            Room.databaseBuilder(context, MemeoryDatabase::class.java, "memeory.db").build()
    }
}
