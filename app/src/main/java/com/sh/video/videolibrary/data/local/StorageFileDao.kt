package com.sh.video.videolibrary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface StorageFileDao {

    @Query("""
        SELECT s.id as id, s.name as name 
        FROM storage_files sf 
        JOIN storages s ON s.id = sf.storageId 
        WHERE sf.fileId = :fileId 
        ORDER BY s.name
    """)
    suspend fun getStoragesByFileId(fileId: Long): List<StorageNameRow>

    @Query("SELECT 1 FROM storage_files WHERE fileId = :fileId AND storageId = :storageId LIMIT 1")
    suspend fun exists(fileId: Long, storageId: Long): Boolean?

    @Insert
    suspend fun insert(entity: StorageFileEntity): Long

    @Query("DELETE FROM storage_files WHERE fileId = :fileId")
    suspend fun deleteByFileId(fileId: Long)

    @Query("DELETE FROM storage_files WHERE storageId = :storageId")
    suspend fun deleteByStorageId(storageId: Long)

    @Query("SELECT COUNT(*) FROM storage_files WHERE storageId = :storageId")
    suspend fun getFileCountByStorageId(storageId: Long): Int
}

data class StorageNameRow(
    val id: Long,
    val name: String
)
