package com.sh.video.videolibrary.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tmdbId: Long,
    val title: String,
    val originalTitle: String,
    val genres: String,
    val rating: Double,
    val overview: String,
    val releaseDate: String,
    val posterPath: String?,
    val personalRating: Int?,
    val createdAt: String = ""
)
