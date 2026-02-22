package com.sh.video.videolibrary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MovieActorDao {

    @Query("SELECT actorId FROM movie_actors WHERE movieId = :movieId")
    suspend fun getActorIdsByMovieId(movieId: Long): List<Long>

    @Query("""
        SELECT a.* FROM actors a 
        JOIN movie_actors ma ON a.id = ma.actorId 
        WHERE ma.movieId = :movieId 
        ORDER BY ma.creditOrder IS NULL, ma.creditOrder ASC, a.name ASC
    """)
    suspend fun getActorsByMovieId(movieId: Long): List<ActorEntity>

    @Query("""
        SELECT ma.creditOrder FROM movie_actors ma 
        WHERE ma.movieId = :movieId AND ma.actorId = :actorId
    """)
    suspend fun getCreditOrder(movieId: Long, actorId: Long): Int?

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
    suspend fun setMovieActors(movieId: Long, actorLinks: List<Pair<Long, Int>>) {
        deleteByMovieId(movieId)
        actorLinks.forEach { (actorId, order) ->
            insert(MovieActorEntity(movieId = movieId, actorId = actorId, creditOrder = order))
        }
    }

    @Query("SELECT COUNT(*) FROM movie_actors WHERE actorId = :actorId")
    suspend fun getMovieCountByActorId(actorId: Long): Int

    @Query("""
        SELECT ma.movieId, a.id as actorId, a.tmdbPersonId, a.name as actorName
        FROM actors a
        JOIN movie_actors ma ON a.id = ma.actorId
        WHERE ma.movieId IN (:movieIds)
        ORDER BY ma.movieId, ma.creditOrder IS NULL, ma.creditOrder ASC
    """)
    suspend fun getActorsForMovies(movieIds: List<Long>): List<ActorForMovieRow>
}

data class ActorForMovieRow(
    val movieId: Long,
    val actorId: Long,
    val tmdbPersonId: Long,
    val actorName: String
)
