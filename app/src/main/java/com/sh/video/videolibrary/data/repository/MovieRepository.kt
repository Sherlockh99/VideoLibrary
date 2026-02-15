package com.sh.video.videolibrary.data.repository

import android.content.Context
import com.google.gson.Gson
import com.sh.video.videolibrary.data.local.AppDatabase
import com.sh.video.videolibrary.data.local.DatabaseProvider
import com.sh.video.videolibrary.data.local.MovieDao
import com.sh.video.videolibrary.data.local.MovieEntity
import com.sh.video.videolibrary.data.local.MovieFileDao
import com.sh.video.videolibrary.data.local.MovieFileEntity
import com.sh.video.videolibrary.data.local.MovieFileWithStorage
import com.sh.video.videolibrary.data.local.StorageDao
import com.sh.video.videolibrary.data.local.StorageEntity
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
    private val storageDao: StorageDao = db.storageDao()
    private val movieFileDao: MovieFileDao = db.movieFileDao()
    private val gson = Gson()

    fun getAllMovies(): Flow<List<MovieEntity>> = movieDao.getAllFlow()

    fun searchMovies(
        title: String?,
        genre: String?,
        minRating: Double?,
        maxRating: Double?,
        description: String?,
        personalRating: Int?,
        storageName: String? = null
    ): Flow<List<MovieEntity>> = movieDao.search(
        title, genre, minRating, maxRating, description, personalRating, storageName
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

    fun getAllStorages(): Flow<List<StorageEntity>> = storageDao.getAllFlow()

    suspend fun getAllStoragesSync(): List<StorageEntity> = storageDao.getAllSync()

    suspend fun addStorage(name: String): Long = storageDao.insert(StorageEntity(name = name))

    suspend fun removeStorage(id: Long) {
        storageDao.deleteById(id)
    }

    suspend fun getMovieFilesByMovieId(movieId: Long): List<MovieFileWithStorage> =
        movieFileDao.getByMovieId(movieId)

    suspend fun getStorageNamesByMovieId(movieId: Long): List<String> =
        movieFileDao.getStorageNamesByMovieId(movieId)

    suspend fun addMovieFile(movieId: Long, storageId: Long): Boolean {
        if (movieFileDao.exists(movieId, storageId) == true) return false
        movieFileDao.insert(MovieFileEntity(movieId = movieId, storageId = storageId))
        return true
    }

    suspend fun removeMovieFile(movieFileId: Long) {
        movieFileDao.deleteById(movieFileId)
    }

    suspend fun exportToOutputStream(outputStream: java.io.OutputStream): Int {
        val movies = movieDao.getAllSync()
        exportMoviesToStream(outputStream, movies)
        return movies.size
    }

    private suspend fun exportMoviesToStream(outputStream: java.io.OutputStream, movies: List<MovieEntity>) {
        val storages = storageDao.getAllSync()
        val data = ExportFormat(
            format = "videolibrary",
            version = 1,
            exportedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            source = "android",
            storages = storages.map { ExportFormat.StorageExport(id = it.id.toInt(), name = it.name) },
            movies = movies.map { m ->
                val storageNames = movieFileDao.getStorageNamesByMovieId(m.id)
                ExportFormat.MovieExport(
                    tmdbId = m.tmdbId.toInt(),
                    title = m.title,
                    originalTitle = m.originalTitle,
                    genres = m.genres,
                    rating = m.rating,
                    overview = m.overview,
                    releaseDate = m.releaseDate,
                    posterPath = m.posterPath,
                    personalRating = m.personalRating,
                    storageNames = storageNames
                )
            }
        )
        outputStream.write(gson.toJson(data).toByteArray(Charsets.UTF_8))
    }

    suspend fun importFromFileContent(json: String, replaceDuplicates: Boolean): Pair<Int, Int> {
        val data = gson.fromJson(json, ExportFormat::class.java)
        if (data.format != "videolibrary") throw IllegalArgumentException("Неверный формат файла")

        for (s in data.storages.orEmpty()) {
            if (storageDao.getByName(s.name) == null) {
                storageDao.insert(StorageEntity(name = s.name))
            }
        }

        var added = 0
        var skipped = 0
        for (m in data.movies) {
            val exists = movieDao.existsByTmdbId(m.tmdbId.toLong())
            val storageNames = m.storageNames ?: emptyList()

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
                        movieFileDao.deleteByMovieId(existing.id)
                        for (sn in storageNames) {
                            storageDao.getByName(sn)?.let { storage ->
                                movieFileDao.insert(MovieFileEntity(movieId = existing.id, storageId = storage.id))
                            }
                        }
                        added++
                    } else skipped++
                } else skipped++
                continue
            }
            val id = movieDao.insert(
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
            for (sn in storageNames) {
                storageDao.getByName(sn)?.let { storage ->
                    movieFileDao.insert(MovieFileEntity(movieId = id, storageId = storage.id))
                }
            }
            added++
        }
        return Pair(added, skipped)
    }
}
