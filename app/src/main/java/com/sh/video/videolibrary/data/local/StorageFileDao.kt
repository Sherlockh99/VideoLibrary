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

    @Query("DELETE FROM storage_files WHERE fileId = :fileId AND storageId = :storageId")
    suspend fun deleteByFileIdAndStorageId(fileId: Long, storageId: Long)

    @Query("SELECT COUNT(*) FROM storage_files WHERE storageId = :storageId")
    suspend fun getFileCountByStorageId(storageId: Long): Int

    @Query("""
        SELECT f.id as fileId, f.name as fileName, f.size as fileSize, f.movieId as movieId,
               m.title as movieTitle, m.posterPath as posterPath, m.releaseDate as releaseDate,
               m.genres as genres, m.rating as rating, m.personalRating as personalRating
        FROM storage_files sf
        JOIN files f ON f.id = sf.fileId
        JOIN movies m ON m.id = f.movieId
        WHERE sf.storageId = :storageId
        ORDER BY m.title, f.name
    """)
    suspend fun getFilesByStorageId(storageId: Long): List<FileOnStorageRow>
}

data class StorageNameRow(
    val id: Long,
    val name: String
)

data class FileOnStorageRow(
    val fileId: Long,
    val fileName: String,
    val fileSize: Long,
    val movieId: Long,
    val movieTitle: String,
    val posterPath: String?,
    val releaseDate: String,
    val genres: String,
    val rating: Double,
    val personalRating: Int?
)
