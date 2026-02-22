package com.sh.video.videolibrary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MovieEntity::class,
        StorageEntity::class,
        FileEntity::class,
        StorageFileEntity::class,
        CategoryEntity::class,
        MovieCategoryEntity::class,
        ActorEntity::class,
        MovieActorEntity::class
    ],
    version = 7
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun storageDao(): StorageDao
    abstract fun fileDao(): FileDao
    abstract fun storageFileDao(): StorageFileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun movieCategoryDao(): MovieCategoryDao
    abstract fun actorDao(): ActorDao
    abstract fun movieActorDao(): MovieActorDao
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

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS movie_categories (
                movieId INTEGER NOT NULL,
                categoryId INTEGER NOT NULL,
                PRIMARY KEY(movieId, categoryId),
                FOREIGN KEY(movieId) REFERENCES movies(id) ON DELETE CASCADE,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_movie_categories_movieId ON movie_categories(movieId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_movie_categories_categoryId ON movie_categories(categoryId)")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE movies ADD COLUMN mediaType TEXT NOT NULL DEFAULT 'movie'")
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS actors (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tmdbPersonId INTEGER NOT NULL UNIQUE,
                name TEXT NOT NULL
            )
        """)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_actors_tmdbPersonId ON actors(tmdbPersonId)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS movie_actors (
                movieId INTEGER NOT NULL,
                actorId INTEGER NOT NULL,
                PRIMARY KEY(movieId, actorId),
                FOREIGN KEY(movieId) REFERENCES movies(id) ON DELETE CASCADE,
                FOREIGN KEY(actorId) REFERENCES actors(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_movie_actors_movieId ON movie_actors(movieId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_movie_actors_actorId ON movie_actors(actorId)")
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_actors_tmdbPersonId ON actors(tmdbPersonId)")
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .build()
            .also { _database = it }
    }
}
