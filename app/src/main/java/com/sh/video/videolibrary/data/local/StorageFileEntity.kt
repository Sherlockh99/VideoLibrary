package com.sh.video.videolibrary.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "storage_files",
    foreignKeys = [
        ForeignKey(
            entity = StorageEntity::class,
            parentColumns = ["id"],
            childColumns = ["storageId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["id"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("storageId"), Index("fileId")]
)
data class StorageFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val storageId: Long,
    val fileId: Long
)
