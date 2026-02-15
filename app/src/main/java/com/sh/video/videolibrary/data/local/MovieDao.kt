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

    @Query("SELECT EXISTS(SELECT 1 FROM movies WHERE tmdbId = :tmdbId)")
    suspend fun existsByTmdbId(tmdbId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(movie: MovieEntity): Long

    @Update
    suspend fun update(movie: MovieEntity)

    @Query("UPDATE movies SET personalRating = :rating WHERE id = :id")
    suspend fun updatePersonalRating(id: Long, rating: Int)

    @Query("""
        SELECT * FROM movies 
        WHERE (:title IS NULL OR title LIKE '%' || :title || '%' OR originalTitle LIKE '%' || :title || '%')
        AND (:genre IS NULL OR genres LIKE '%' || :genre || '%')
        AND (:minRating IS NULL OR rating >= :minRating)
        AND (:maxRating IS NULL OR rating <= :maxRating)
        AND (:description IS NULL OR overview LIKE '%' || :description || '%')
        AND (:personalRating IS NULL OR personalRating = :personalRating)
        ORDER BY title
    """)
    fun search(
        title: String?,
        genre: String?,
        minRating: Double?,
        maxRating: Double?,
        description: String?,
        personalRating: Int?
    ): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies ORDER BY title")
    suspend fun getAllSync(): List<MovieEntity>
}
