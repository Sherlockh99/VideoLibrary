package com.sh.video.videolibrary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MovieEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
}

object DatabaseProvider {
    private var _database: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return _database ?: Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "videolibrary.db"
        ).build().also { _database = it }
    }
}
