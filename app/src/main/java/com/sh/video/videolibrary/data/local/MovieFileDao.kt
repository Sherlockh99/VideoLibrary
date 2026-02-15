package com.sh.video.videolibrary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MovieFileDao {

    @Query("""
        SELECT mf.id as id, mf.movieId as movieId, mf.storageId as storageId, mf.createdAt as createdAt, s.name as storageName 
        FROM movie_files mf 
        JOIN storages s ON s.id = mf.storageId 
        WHERE mf.movieId = :movieId 
        ORDER BY s.name
    """)
    suspend fun getByMovieId(movieId: Long): List<MovieFileWithStorage>

    @Query("SELECT s.name FROM movie_files mf JOIN storages s ON s.id = mf.storageId WHERE mf.movieId = :movieId ORDER BY s.name")
    suspend fun getStorageNamesByMovieId(movieId: Long): List<String>

    @Query("SELECT 1 FROM movie_files WHERE movieId = :movieId AND storageId = :storageId LIMIT 1")
    suspend fun exists(movieId: Long, storageId: Long): Boolean?

    @Insert
    suspend fun insert(movieFile: MovieFileEntity): Long

    @Query("DELETE FROM movie_files WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM movie_files WHERE movieId = :movieId")
    suspend fun deleteByMovieId(movieId: Long)
}

data class MovieFileWithStorage(
    val id: Long,
    val movieId: Long,
    val storageId: Long,
    val createdAt: String,
    val storageName: String
)
