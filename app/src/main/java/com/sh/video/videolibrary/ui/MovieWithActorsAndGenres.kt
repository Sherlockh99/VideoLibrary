package com.sh.video.videolibrary.ui

import com.sh.video.videolibrary.data.local.GenreEntity
import com.sh.video.videolibrary.data.local.MovieEntity

data class MovieWithActorsAndGenres(
    val movie: MovieEntity,
    val topActorNames: List<String>,
    val genres: List<GenreEntity>,
    val categoryCount: Int = 0
)
