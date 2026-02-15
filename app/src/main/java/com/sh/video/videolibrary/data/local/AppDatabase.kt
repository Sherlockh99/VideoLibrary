package com.sh.video.videolibrary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MovieEntity::class, StorageEntity::class, FileEntity::class, StorageFileEntity::class],
    version = 3
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun storageDao(): StorageDao
    abstract fun fileDao(): FileDao
    abstract fun storageFileDao(): StorageFileDao
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

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS files (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                size INTEGER NOT NULL DEFAULT 0,
                movieId INTEGER NOT NULL,
                FOREIGN KEY(movieId) REFERENCES movies(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_files_movieId ON files(movieId)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS storage_files (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                storageId INTEGER NOT NULL,
                fileId INTEGER NOT NULL,
                FOREIGN KEY(storageId) REFERENCES storages(id) ON DELETE CASCADE,
                FOREIGN KEY(fileId) REFERENCES files(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_storage_files_storageId ON storage_files(storageId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_storage_files_fileId ON storage_files(fileId)")
        db.execSQL("DROP TABLE IF EXISTS movie_files")
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
            .also { _database = it }
    }
}
