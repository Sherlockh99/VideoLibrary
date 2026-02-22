package com.sh.video.videolibrary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MovieActorDao {

    @Query("SELECT actorId FROM movie_actors WHERE movieId = :movieId")
    suspend fun getActorIdsByMovieId(movieId: Long): List<Long>

    @Query("SELECT a.* FROM actors a JOIN movie_actors ma ON a.id = ma.actorId WHERE ma.movieId = :movieId ORDER BY a.name")
    suspend fun getActorsByMovieId(movieId: Long): List<ActorEntity>

    @Query("SELECT movieId FROM movie_actors WHERE actorId = :actorId")
    suspend fun getMovieIdsByActorId(actorId: Long): List<Long>

    @Query("""
        SELECT m.* FROM movies m 
        JOIN movie_actors ma ON m.id = ma.movieId 
        WHERE ma.actorId = :actorId 
        ORDER BY m.title
    """)
    suspend fun getMoviesByActorId(actorId: Long): List<MovieEntity>

    @Insert
    suspend fun insert(entity: MovieActorEntity)

    @Query("DELETE FROM movie_actors WHERE movieId = :movieId")
    suspend fun deleteByMovieId(movieId: Long)

    @Transaction
    suspend fun setMovieActors(movieId: Long, actorIds: List<Long>) {
        deleteByMovieId(movieId)
        actorIds.forEach { actorId ->
            insert(MovieActorEntity(movieId = movieId, actorId = actorId))
        }
    }

    @Query("SELECT COUNT(*) FROM movie_actors WHERE actorId = :actorId")
    suspend fun getMovieCountByActorId(actorId: Long): Int
}
