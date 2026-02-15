package com.sh.video.videolibrary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface StorageDao {

    @Query("SELECT * FROM storages ORDER BY name")
    fun getAllFlow(): kotlinx.coroutines.flow.Flow<List<StorageEntity>>

    @Query("SELECT * FROM storages ORDER BY name")
    suspend fun getAllSync(): List<StorageEntity>

    @Query("SELECT * FROM storages WHERE id = :id")
    suspend fun getById(id: Long): StorageEntity?

    @Query("SELECT * FROM storages WHERE name = :name")
    suspend fun getByName(name: String): StorageEntity?

    @Insert
    suspend fun insert(storage: StorageEntity): Long

    @Update
    suspend fun update(storage: StorageEntity)

    @Query("DELETE FROM storages WHERE id = :id")
    suspend fun deleteById(id: Long)
}
