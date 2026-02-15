package com.sh.video.videolibrary.data.repository

import android.content.Context
import com.google.gson.Gson
import com.sh.video.videolibrary.data.local.AppDatabase
import com.sh.video.videolibrary.data.local.DatabaseProvider
import com.sh.video.videolibrary.data.local.MovieDao
import com.sh.video.videolibrary.data.local.MovieEntity
import com.sh.video.videolibrary.data.local.FileDao
import com.sh.video.videolibrary.data.local.FileEntity
import com.sh.video.videolibrary.data.local.StorageDao
import com.sh.video.videolibrary.data.local.StorageFileDao
import com.sh.video.videolibrary.data.local.StorageFileEntity
import com.sh.video.videolibrary.data.local.StorageEntity
import com.sh.video.videolibrary.data.remote.TmdbApi
import com.sh.video.videolibrary.data.remote.TmdbMovieDetails
import com.sh.video.videolibrary.export.ExportFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class FileWithStorages(val file: FileEntity, val storageNames: List<String>)

data class StorageWithFileCount(val storage: StorageEntity, val fileCount: Int)

class MovieRepository(
    private val context: Context,
    private val tmdbApi: TmdbApi
) {

    private val db: AppDatabase = DatabaseProvider.getDatabase(context)
    private val movieDao: MovieDao = db.movieDao()
    private val storageDao: StorageDao = db.storageDao()
    private val fileDao: FileDao = db.fileDao()
    private val storageFileDao: StorageFileDao = db.storageFileDao()
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

    fun getStoragesWithFileCounts(): Flow<List<StorageWithFileCount>> = flow {
        storageDao.getAllFlow().collect { storages ->
            val withCounts = storages.map { s ->
                StorageWithFileCount(s, storageFileDao.getFileCountByStorageId(s.id))
            }
            emit(withCounts)
        }
    }

    suspend fun getAllStoragesSync(): List<StorageEntity> = storageDao.getAllSync()

    suspend fun addStorage(name: String): Long = storageDao.insert(StorageEntity(name = name))

    suspend fun getFileCountByStorageId(storageId: Long): Int =
        storageFileDao.getFileCountByStorageId(storageId)

    /** Удаляет хранилище. Возвращает true если удалено. Нельзя удалить хранилище, к которому привязаны файлы. */
    suspend fun removeStorage(id: Long): Boolean {
        if (storageFileDao.getFileCountByStorageId(id) > 0) return false
        storageDao.deleteById(id)
        return true
    }

    suspend fun getFilesByMovieId(movieId: Long): List<FileWithStorages> {
        val files = fileDao.getByMovieId(movieId)
        return files.map { file ->
            val storages = storageFileDao.getStoragesByFileId(file.id)
            FileWithStorages(file, storages.map { it.name })
        }
    }

    suspend fun addFile(movieId: Long, name: String, size: Long = 0): Long =
        fileDao.insert(FileEntity(name = name, size = size, movieId = movieId))

    suspend fun addFileToStorage(fileId: Long, storageId: Long): Boolean {
        if (storageFileDao.exists(fileId, storageId) == true) return false
        storageFileDao.insert(StorageFileEntity(fileId = fileId, storageId = storageId))
        return true
    }

    suspend fun removeFile(fileId: Long) {
        fileDao.deleteById(fileId)
    }

    /** Удаляет фильм из коллекции. Возвращает true если удалён. Удалять можно только фильм без файлов. */
    suspend fun removeMovie(movieId: Long): Boolean {
        val files = fileDao.getByMovieId(movieId)
        if (files.isNotEmpty()) return false
        movieDao.deleteById(movieId)
        return true
    }

    suspend fun updateFile(fileId: Long, name: String, size: Long, storageIds: List<Long>) {
        val file = fileDao.getById(fileId) ?: return
        fileDao.update(file.copy(name = name, size = size))
        storageFileDao.deleteByFileId(fileId)
        storageIds.forEach { storageFileDao.insert(StorageFileEntity(fileId = fileId, storageId = it)) }
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
                val filesData = getFilesByMovieId(m.id).map { fws ->
                    ExportFormat.FileExport(
                        name = fws.file.name,
                        size = fws.file.size,
                        storageNames = fws.storageNames
                    )
                }
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
                    files = filesData
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
            val filesData = m.files ?: emptyList()

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
                        fileDao.deleteByMovieId(existing.id)
                        for (f in filesData) {
                            val fileId = fileDao.insert(FileEntity(name = f.name, size = f.size.toLong(), movieId = existing.id))
                            for (sn in f.storageNames.orEmpty()) {
                                storageDao.getByName(sn)?.let { storage ->
                                    storageFileDao.insert(StorageFileEntity(fileId = fileId, storageId = storage.id))
                                }
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
            for (f in filesData) {
                val fileId = fileDao.insert(FileEntity(name = f.name, size = f.size.toLong(), movieId = id))
                for (sn in f.storageNames.orEmpty()) {
                    storageDao.getByName(sn)?.let { storage ->
                        storageFileDao.insert(StorageFileEntity(fileId = fileId, storageId = storage.id))
                    }
                }
            }
            added++
        }
        return Pair(added, skipped)
    }
}
