package com.sh.video.videolibrary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActorDao {

    @Query("SELECT * FROM actors ORDER BY name")
    fun getAllFlow(): Flow<List<ActorEntity>>

    @Query("SELECT * FROM actors ORDER BY name")
    suspend fun getAllSync(): List<ActorEntity>

    @Query("SELECT * FROM actors WHERE id = :id")
    suspend fun getById(id: Long): ActorEntity?

    @Query("SELECT * FROM actors WHERE tmdbPersonId = :tmdbPersonId")
    suspend fun getByTmdbPersonId(tmdbPersonId: Long): ActorEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(actor: ActorEntity): Long

    @Update
    suspend fun update(actor: ActorEntity)

    @Query("SELECT id FROM actors WHERE tmdbPersonId = :tmdbPersonId")
    suspend fun getIdByTmdbPersonId(tmdbPersonId: Long): Long?
}
