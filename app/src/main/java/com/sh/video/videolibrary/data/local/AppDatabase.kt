package com.sh.video.videolibrary.data.local

import android.content.Context
import android.database.Cursor
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteProgram
import androidx.sqlite.db.SupportSQLiteQuery

@Database(
    entities = [
        MovieEntity::class,
        StorageEntity::class,
        FileEntity::class,
        StorageFileEntity::class,
        CategoryEntity::class,
        MovieCategoryEntity::class,
        ActorEntity::class,
        MovieActorEntity::class,
        GenreEntity::class,
        MovieGenreEntity::class
    ],
    version = 9
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
    abstract fun genreDao(): GenreDao
    abstract fun movieGenreDao(): MovieGenreDao
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

private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE movie_actors ADD COLUMN creditOrder INTEGER")
    }
}

private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS genres (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tmdbGenreId INTEGER,
                name TEXT NOT NULL
            )
        """)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_genres_tmdbGenreId ON genres(tmdbGenreId)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS movie_genres (
                movieId INTEGER NOT NULL,
                genreId INTEGER NOT NULL,
                PRIMARY KEY(movieId, genreId),
                FOREIGN KEY(movieId) REFERENCES movies(id) ON DELETE CASCADE,
                FOREIGN KEY(genreId) REFERENCES genres(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_movie_genres_movieId ON movie_genres(movieId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_movie_genres_genreId ON movie_genres(genreId)")

        // Migrate existing genres string to new tables
        val moviesCursor: Cursor = db.query(object : SupportSQLiteQuery {
            override val sql: String get() = "SELECT id, genres FROM movies"
            override val argCount: Int get() = 0
            override fun bindTo(statement: SupportSQLiteProgram) {}
        })
        val genreNameToId = mutableMapOf<String, Long>()
        try {
            while (moviesCursor.moveToNext()) {
                val movieId = moviesCursor.getLong(0)
                val genresStr = moviesCursor.getString(1) ?: ""
                val names = genresStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                for (name in names) {
                    val genreId = genreNameToId.getOrPut(name) {
                        db.execSQL("INSERT INTO genres (tmdbGenreId, name) VALUES (NULL, ?)", arrayOf(name))
                        val idCursor: Cursor = db.query(object : SupportSQLiteQuery {
                            override val sql: String get() = "SELECT id FROM genres WHERE name = ? ORDER BY id DESC LIMIT 1"
                            override val argCount: Int get() = 1
                            override fun bindTo(statement: SupportSQLiteProgram) {
                                statement.bindString(1, name)
                            }
                        })
                        idCursor.use {
                            if (it.moveToFirst()) it.getLong(0) else 0L
                        }
                    }
                    db.execSQL("INSERT OR IGNORE INTO movie_genres (movieId, genreId) VALUES (?, ?)", arrayOf(movieId, genreId))
                }
            }
        } finally {
            moviesCursor.close()
        }
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
            .build()
            .also { _database = it }
    }
}
