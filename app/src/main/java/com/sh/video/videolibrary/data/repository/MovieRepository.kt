package com.sh.video.videolibrary.data.repository

import android.content.Context
import com.google.gson.Gson
import com.sh.video.videolibrary.data.local.AppDatabase
import com.sh.video.videolibrary.data.local.DatabaseProvider
import com.sh.video.videolibrary.data.local.MovieDao
import com.sh.video.videolibrary.data.local.MovieEntity
import com.sh.video.videolibrary.data.remote.TmdbApi
import com.sh.video.videolibrary.data.remote.TmdbMovieDetails
import com.sh.video.videolibrary.export.ExportFormat
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MovieRepository(
    private val context: Context,
    private val tmdbApi: TmdbApi
) {

    private val db: AppDatabase = DatabaseProvider.getDatabase(context)
    private val movieDao: MovieDao = db.movieDao()
    private val gson = Gson()

    fun getAllMovies(): Flow<List<MovieEntity>> = movieDao.getAllFlow()

    fun searchMovies(
        title: String?,
        genre: String?,
        minRating: Double?,
        maxRating: Double?,
        description: String?,
        personalRating: Int?
    ): Flow<List<MovieEntity>> = movieDao.search(
        title, genre, minRating, maxRating, description, personalRating
    )

    suspend fun searchTmdb(query: String) = tmdbApi.searchMovies(query = query)

    suspend fun getTmdbDetails(movieId: Long) = tmdbApi.getMovieDetails(movieId)

    suspend fun addMovie(details: TmdbMovieDetails): Long {
        val exists = movieDao.existsByTmdbId(details.id)
        if (exists) return -1

        val genres = details.genres?.joinToString(", ") { it.name } ?: ""
        val entity = MovieEntity(
            tmdbId = details.id,
            title = details.title,
            originalTitle = details.originalTitle ?: "",
            genres = genres,
            rating = details.voteAverage ?: 0.0,
            overview = details.overview ?: "",
            releaseDate = details.releaseDate ?: "",
            posterPath = details.posterPath,
            personalRating = null,
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        return movieDao.insert(entity)
    }

    suspend fun updatePersonalRating(id: Long, rating: Int) {
        movieDao.updatePersonalRating(id, rating)
    }

    suspend fun exportToOutputStream(outputStream: java.io.OutputStream): Int {
        val movies = movieDao.getAllSync()
        exportMoviesToStream(outputStream, movies)
        return movies.size
    }

    private fun exportMoviesToStream(outputStream: java.io.OutputStream, movies: List<MovieEntity>) {
        val data = ExportFormat(
            format = "videolibrary",
            version = 1,
            exportedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            source = "android",
            movies = movies.map { m ->
                ExportFormat.MovieExport(
                    tmdbId = m.tmdbId.toInt(),
                    title = m.title,
                    originalTitle = m.originalTitle,
                    genres = m.genres,
                    rating = m.rating,
                    overview = m.overview,
                    releaseDate = m.releaseDate,
                    posterPath = m.posterPath,
                    personalRating = m.personalRating
                )
            }
        )
        outputStream.write(gson.toJson(data).toByteArray(Charsets.UTF_8))
    }

    suspend fun importFromFileContent(json: String, replaceDuplicates: Boolean): Pair<Int, Int> {
        val data = gson.fromJson(json, ExportFormat::class.java)
        if (data.format != "videolibrary") throw IllegalArgumentException("Неверный формат файла")
        var added = 0
        var skipped = 0
        for (m in data.movies) {
            val exists = movieDao.existsByTmdbId(m.tmdbId.toLong())
            if (exists) {
                if (replaceDuplicates) {
                    val all = movieDao.getAllSync()
                    val existing = all.find { it.tmdbId == m.tmdbId.toLong() }
                    if (existing != null) {
                        val updated = existing.copy(
                            title = m.title,
                            originalTitle = m.originalTitle,
                            genres = m.genres,
                            rating = m.rating,
                            overview = m.overview,
                            releaseDate = m.releaseDate,
                            posterPath = m.posterPath,
                            personalRating = m.personalRating
                        )
                        movieDao.update(updated)
                        added++
                    } else skipped++
                } else skipped++
                continue
            }
            movieDao.insert(
                MovieEntity(
                    tmdbId = m.tmdbId.toLong(),
                    title = m.title,
                    originalTitle = m.originalTitle,
                    genres = m.genres,
                    rating = m.rating,
                    overview = m.overview,
                    releaseDate = m.releaseDate,
                    posterPath = m.posterPath,
                    personalRating = m.personalRating,
                    createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            )
            added++
        }
        return Pair(added, skipped)
    }
}
