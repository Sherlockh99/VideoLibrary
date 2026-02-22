package com.sh.video.videolibrary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    @Query("SELECT * FROM movies ORDER BY title")
    fun getAllFlow(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getById(id: Long): MovieEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM movies WHERE tmdbId = :tmdbId AND mediaType = :mediaType)")
    suspend fun existsByTmdbIdAndMediaType(tmdbId: Long, mediaType: String): Boolean

    @Query("SELECT * FROM movies WHERE tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun getByTmdbIdAndMediaType(tmdbId: Long, mediaType: String): MovieEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(movie: MovieEntity): Long

    @Update
    suspend fun update(movie: MovieEntity)

    @Query("UPDATE movies SET personalRating = :rating WHERE id = :id")
    suspend fun updatePersonalRating(id: Long, rating: Int)

    @Query("""
        SELECT * FROM movies 
        WHERE (:title IS NULL OR title LIKE '%' || :title || '%' OR originalTitle LIKE '%' || :title || '%')
        AND (:genre IS NULL OR id IN (
            SELECT mg.movieId FROM movie_genres mg
            JOIN genres g ON g.id = mg.genreId
            WHERE g.name LIKE '%' || :genre || '%'
        ))
        AND (:minRating IS NULL OR rating >= :minRating)
        AND (:maxRating IS NULL OR rating <= :maxRating)
        AND (:description IS NULL OR overview LIKE '%' || :description || '%')
        AND (:personalRating IS NULL OR personalRating = :personalRating)
        AND (:storageName IS NULL OR id IN (
            SELECT f.movieId FROM files f 
            JOIN storage_files sf ON sf.fileId = f.id 
            JOIN storages s ON s.id = sf.storageId AND s.name = :storageName
        ))
        ORDER BY title
    """)
    fun search(
        title: String?,
        genre: String?,
        minRating: Double?,
        maxRating: Double?,
        description: String?,
        personalRating: Int?,
        storageName: String? = null
    ): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies ORDER BY title")
    suspend fun getAllSync(): List<MovieEntity>

    @Query("DELETE FROM movies WHERE id = :id")
    suspend fun deleteById(id: Long)
}
