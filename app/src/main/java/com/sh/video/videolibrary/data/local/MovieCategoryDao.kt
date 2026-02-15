package com.sh.video.videolibrary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MovieCategoryDao {

    @Query("SELECT categoryId FROM movie_categories WHERE movieId = :movieId")
    suspend fun getCategoryIdsByMovieId(movieId: Long): List<Long>

    @Query("SELECT c.* FROM categories c JOIN movie_categories mc ON c.id = mc.categoryId WHERE mc.movieId = :movieId ORDER BY c.name")
    suspend fun getCategoriesByMovieId(movieId: Long): List<CategoryEntity>

    @Insert
    suspend fun insert(entity: MovieCategoryEntity)

    @Query("DELETE FROM movie_categories WHERE movieId = :movieId AND categoryId = :categoryId")
    suspend fun delete(movieId: Long, categoryId: Long)

    @Query("DELETE FROM movie_categories WHERE movieId = :movieId")
    suspend fun deleteByMovieId(movieId: Long)

    @Query("SELECT COUNT(*) FROM movie_categories WHERE categoryId = :categoryId")
    suspend fun getMovieCountByCategoryId(categoryId: Long): Int

    @Query("""
        SELECT m.* FROM movies m 
        JOIN movie_categories mc ON m.id = mc.movieId 
        WHERE mc.categoryId = :categoryId 
        ORDER BY m.title
    """)
    suspend fun getMoviesByCategoryId(categoryId: Long): List<MovieEntity>

    @Transaction
    suspend fun setMovieCategories(movieId: Long, categoryIds: List<Long>) {
        deleteByMovieId(movieId)
        categoryIds.forEach { categoryId ->
            insert(MovieCategoryEntity(movieId = movieId, categoryId = categoryId))
        }
    }
}
