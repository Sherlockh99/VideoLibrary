package com.sh.video.videolibrary.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "actors", indices = [Index(value = ["tmdbPersonId"], unique = true)])
data class ActorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tmdbPersonId: Long,
    val name: String
)
