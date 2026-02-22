package com.sh.video.videolibrary.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "genres", indices = [Index(value = ["tmdbGenreId"], unique = true)])
data class GenreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** ID жанра из TMDb — для перевода названий на русский в будущем */
    val tmdbGenreId: Int?,
    val name: String
)
