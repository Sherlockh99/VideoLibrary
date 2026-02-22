package com.sh.video.videolibrary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface GenreDao {

    @Query("SELECT * FROM genres ORDER BY name")
    suspend fun getAllSync(): List<GenreEntity>

    @Query("SELECT * FROM genres WHERE id = :id")
    suspend fun getById(id: Long): GenreEntity?

    @Query("SELECT * FROM genres WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): GenreEntity?

    @Query("SELECT id FROM genres WHERE tmdbGenreId = :tmdbGenreId LIMIT 1")
    suspend fun getIdByTmdbGenreId(tmdbGenreId: Int): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(genre: GenreEntity): Long

    @Update
    suspend fun update(genre: GenreEntity)
}
