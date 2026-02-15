package com.sh.video.videolibrary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MovieEntity::class, StorageEntity::class, MovieFileEntity::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun storageDao(): StorageDao
    abstract fun movieFileDao(): MovieFileDao
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS storages (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                isAvailable INTEGER NOT NULL DEFAULT 1
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS movie_files (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                movieId INTEGER NOT NULL,
                storageId INTEGER NOT NULL,
                createdAt TEXT NOT NULL DEFAULT '',
                FOREIGN KEY(movieId) REFERENCES movies(id) ON DELETE CASCADE,
                FOREIGN KEY(storageId) REFERENCES storages(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_movie_files_movieId ON movie_files(movieId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_movie_files_storageId ON movie_files(storageId)")
    }
}

object DatabaseProvider {
    private var _database: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return _database ?: Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "videolibrary.db"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
            .also { _database = it }
    }
}
