package com.sh.video.videolibrary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface FileDao {

    @Query("SELECT * FROM files WHERE movieId = :movieId ORDER BY name")
    suspend fun getByMovieId(movieId: Long): List<FileEntity>

    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun getById(id: Long): FileEntity?

    @Insert
    suspend fun insert(file: FileEntity): Long

    @Update
    suspend fun update(file: FileEntity)

    @Query("DELETE FROM files WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM files WHERE movieId = :movieId")
    suspend fun deleteByMovieId(movieId: Long)
}
