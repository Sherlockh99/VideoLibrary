package com.sh.video.videolibrary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MovieGenreDao {

    @Query("SELECT genreId FROM movie_genres WHERE movieId = :movieId")
    suspend fun getGenreIdsByMovieId(movieId: Long): List<Long>

    @Query("""
        SELECT g.name FROM genres g
        JOIN movie_genres mg ON g.id = mg.genreId
        WHERE mg.movieId = :movieId
        ORDER BY g.name
    """)
    suspend fun getGenreNamesByMovieId(movieId: Long): List<String>

    @Query("""
        SELECT m.id FROM movies m
        JOIN movie_genres mg ON m.id = mg.movieId
        JOIN genres g ON g.id = mg.genreId
        WHERE g.name LIKE '%' || :genre || '%'
    """)
    suspend fun getMovieIdsByGenreFilter(genre: String): List<Long>

    @Query("SELECT movieId FROM movie_genres WHERE genreId = :genreId")
    suspend fun getMovieIdsByGenreId(genreId: Long): List<Long>

    @Insert
    suspend fun insert(entity: MovieGenreEntity)

    @Query("DELETE FROM movie_genres WHERE movieId = :movieId")
    suspend fun deleteByMovieId(movieId: Long)

    @Transaction
    suspend fun setMovieGenres(movieId: Long, genreIds: List<Long>) {
        deleteByMovieId(movieId)
        genreIds.forEach { genreId ->
            insert(MovieGenreEntity(movieId = movieId, genreId = genreId))
        }
    }

    @Query("SELECT COUNT(*) FROM movie_genres WHERE genreId = :genreId")
    suspend fun getMovieCountByGenreId(genreId: Long): Int
}
