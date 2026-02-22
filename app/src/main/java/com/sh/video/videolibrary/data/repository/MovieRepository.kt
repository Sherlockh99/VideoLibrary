package com.sh.video.videolibrary.data.repository

import android.content.Context
import com.google.gson.Gson
import com.sh.video.videolibrary.data.local.ActorDao
import com.sh.video.videolibrary.data.local.ActorEntity
import com.sh.video.videolibrary.data.local.AppDatabase
import com.sh.video.videolibrary.data.local.CategoryDao
import com.sh.video.videolibrary.data.local.CategoryEntity
import com.sh.video.videolibrary.data.local.DatabaseProvider
import com.sh.video.videolibrary.data.local.ActorForMovieRow
import com.sh.video.videolibrary.data.local.MovieActorDao
import com.sh.video.videolibrary.data.local.MovieActorEntity
import com.sh.video.videolibrary.data.local.MovieCategoryDao
import com.sh.video.videolibrary.data.local.MovieCategoryEntity
import com.sh.video.videolibrary.data.local.MovieDao
import com.sh.video.videolibrary.data.local.MovieEntity
import com.sh.video.videolibrary.data.local.FileDao
import com.sh.video.videolibrary.data.local.FileEntity
import com.sh.video.videolibrary.data.local.StorageDao
import com.sh.video.videolibrary.data.local.StorageFileDao
import com.sh.video.videolibrary.data.local.StorageFileEntity
import com.sh.video.videolibrary.data.local.StorageEntity
import com.sh.video.videolibrary.data.local.FileOnStorageRow
import com.sh.video.videolibrary.data.remote.TmdbApi
import com.sh.video.videolibrary.data.remote.TmdbMediaDetails
import com.sh.video.videolibrary.data.remote.TmdbMovieDetails
import com.sh.video.videolibrary.data.remote.TmdbTvDetails
import com.sh.video.videolibrary.export.ExportFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class FileWithStorages(val file: FileEntity, val storageNames: List<String>)

data class StorageWithFileCount(val storage: StorageEntity, val fileCount: Int)

data class CategoryWithMovieCount(val category: CategoryEntity, val movieCount: Int)

data class ActorWithMovieCount(val actor: ActorEntity, val movieCount: Int)

class MovieRepository(
    private val context: Context,
    private val tmdbApi: TmdbApi
) {

    private val db: AppDatabase = DatabaseProvider.getDatabase(context)
    private val movieDao: MovieDao = db.movieDao()
    private val storageDao: StorageDao = db.storageDao()
    private val fileDao: FileDao = db.fileDao()
    private val storageFileDao: StorageFileDao = db.storageFileDao()
    private val categoryDao: CategoryDao = db.categoryDao()
    private val movieCategoryDao: MovieCategoryDao = db.movieCategoryDao()
    private val actorDao: ActorDao = db.actorDao()
    private val movieActorDao: MovieActorDao = db.movieActorDao()
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

    suspend fun searchTmdbMovies(query: String) = tmdbApi.searchMovies(query = query)

    suspend fun searchTmdbTv(query: String) = tmdbApi.searchTv(query = query)

    suspend fun getTmdbMovieDetails(movieId: Long) = tmdbApi.getMovieDetails(movieId)

    suspend fun getTmdbTvDetails(tvId: Long) = tmdbApi.getTvDetails(tvId)

    suspend fun getMovieById(id: Long): MovieEntity? = movieDao.getById(id)

    suspend fun getMovieByTmdbId(tmdbId: Long, mediaType: String = "movie"): MovieEntity? =
        movieDao.getByTmdbIdAndMediaType(tmdbId, mediaType)

    private suspend fun ensureActorsForMovie(movieId: Long, tmdbId: Long, mediaType: String) {
        val credits = runCatching {
            when (mediaType) {
                "movie" -> tmdbApi.getMovieCredits(tmdbId)
                else -> tmdbApi.getTvCredits(tmdbId)
            }
        }.getOrNull() ?: return
        val cast = credits.cast ?: return
        val actorLinks = mutableListOf<Pair<Long, Int>>()
        for (member in cast) {
            val rowId = actorDao.insert(
                ActorEntity(tmdbPersonId = member.id, name = member.name)
            )
            val actorId = if (rowId == -1L) actorDao.getIdByTmdbPersonId(member.id) else rowId
            if (actorId != null && actorId > 0) {
                actorLinks.add(actorId to member.billingOrder)
            }
        }
        if (actorLinks.isNotEmpty()) {
            movieActorDao.setMovieActors(movieId, actorLinks)
        }
    }

    /** Загружает актёров с TMDB для существующего фильма, если в базе их ещё нет. */
    suspend fun loadActorsFromTmdbIfEmpty(movieId: Long, tmdbId: Long, mediaType: String) {
        val existingIds = movieActorDao.getActorIdsByMovieId(movieId)
        if (existingIds.isEmpty()) {
            ensureActorsForMovie(movieId, tmdbId, mediaType)
        }
    }

    suspend fun addMovie(details: TmdbMovieDetails): Long {
        if (movieDao.existsByTmdbIdAndMediaType(details.id, "movie")) return -1
        val genres = details.genres?.joinToString(", ") { it.name } ?: ""
        val entity = MovieEntity(
            tmdbId = details.id,
            mediaType = "movie",
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
        val id = movieDao.insert(entity)
        if (id > 0) {
            ensureActorsForMovie(id, details.id, "movie")
        }
        return id
    }

    suspend fun addTvShow(details: TmdbTvDetails): Long {
        if (movieDao.existsByTmdbIdAndMediaType(details.id, "tv")) return -1
        val genres = details.genres?.joinToString(", ") { it.name } ?: ""
        val entity = MovieEntity(
            tmdbId = details.id,
            mediaType = "tv",
            title = details.name,
            originalTitle = details.originalName ?: "",
            genres = genres,
            rating = details.voteAverage ?: 0.0,
            overview = details.overview ?: "",
            releaseDate = details.firstAirDate ?: "",
            posterPath = details.posterPath,
            personalRating = null,
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        val id = movieDao.insert(entity)
        if (id > 0) {
            ensureActorsForMovie(id, details.id, "tv")
        }
        return id
    }

    suspend fun addMedia(details: TmdbMediaDetails): Long = when (details) {
        is TmdbMediaDetails.Movie -> addMovie(details.data)
        is TmdbMediaDetails.Tv -> addTvShow(details.data)
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

    fun getCategoriesWithMovieCounts(): Flow<List<CategoryWithMovieCount>> = flow {
        categoryDao.getAllFlow().collect { categories ->
            val withCounts = categories.map { c ->
                CategoryWithMovieCount(c, movieCategoryDao.getMovieCountByCategoryId(c.id))
            }
            emit(withCounts)
        }
    }

    suspend fun getAllStoragesSync(): List<StorageEntity> = storageDao.getAllSync()

    suspend fun addStorage(name: String): Long = storageDao.insert(StorageEntity(name = name))

    suspend fun updateStorage(id: Long, name: String) {
        storageDao.getById(id)?.let { storageDao.update(it.copy(name = name)) }
    }

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

    suspend fun getFilesByStorageId(storageId: Long): List<FileOnStorageRow> =
        storageFileDao.getFilesByStorageId(storageId)

    /** Удаляет привязку файла к хранилищу (файл остаётся в карточке фильма). */
    suspend fun removeFileFromStorage(fileId: Long, storageId: Long) {
        storageFileDao.deleteByFileIdAndStorageId(fileId, storageId)
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

    // Categories
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllFlow()

    suspend fun getAllCategoriesSync(): List<CategoryEntity> = categoryDao.getAllSync()

    suspend fun addCategory(name: String): Long {
        val existing = categoryDao.getByName(name)
        return if (existing != null) existing.id else categoryDao.insert(CategoryEntity(name = name))
    }

    suspend fun updateCategory(id: Long, name: String) {
        categoryDao.getById(id)?.let { categoryDao.update(it.copy(name = name)) }
    }

    suspend fun getMovieCountByCategoryId(categoryId: Long): Int =
        movieCategoryDao.getMovieCountByCategoryId(categoryId)

    suspend fun getMoviesByCategoryId(categoryId: Long): List<MovieEntity> =
        movieCategoryDao.getMoviesByCategoryId(categoryId)

    suspend fun removeCategory(id: Long): Boolean {
        if (movieCategoryDao.getMovieCountByCategoryId(id) > 0) return false
        categoryDao.deleteById(id)
        return true
    }

    suspend fun getCategoriesByMovieId(movieId: Long): List<CategoryEntity> =
        movieCategoryDao.getCategoriesByMovieId(movieId)

    suspend fun getCategoryIdsByMovieId(movieId: Long): List<Long> =
        movieCategoryDao.getCategoryIdsByMovieId(movieId)

    fun getActorsWithMovieCounts(): Flow<List<ActorWithMovieCount>> = flow {
        actorDao.getAllFlow().collect { actors ->
            val withCounts = actors.map { a ->
                ActorWithMovieCount(a, movieActorDao.getMovieCountByActorId(a.id))
            }
            emit(withCounts)
        }
    }

    suspend fun getMoviesByActorId(actorId: Long): List<MovieEntity> =
        movieActorDao.getMoviesByActorId(actorId)

    suspend fun getActorById(id: Long): ActorEntity? = actorDao.getById(id)

    /** Возвращает до limit актёров в главных ролях для каждого фильма (по порядку в титрах). */
    suspend fun getTopActorNamesByMovieIds(movieIds: List<Long>, limit: Int = 5): Map<Long, List<String>> {
        if (movieIds.isEmpty()) return emptyMap()
        val rows = movieActorDao.getActorsForMovies(movieIds)
        return rows
            .groupBy { it.movieId }
            .mapValues { (_, list) -> list.take(limit).map { it.actorName } }
    }

    suspend fun setMovieCategories(movieId: Long, categoryIds: List<Long>) {
        movieCategoryDao.setMovieCategories(movieId, categoryIds)
    }

    suspend fun exportToOutputStream(outputStream: java.io.OutputStream): Int {
        val movies = movieDao.getAllSync()
        exportMoviesToStream(outputStream, movies)
        return movies.size
    }

    private suspend fun exportMoviesToStream(outputStream: java.io.OutputStream, movies: List<MovieEntity>) {
        val storages = storageDao.getAllSync()
        val categories = categoryDao.getAllSync()
        val data = ExportFormat(
            format = "videolibrary",
            version = 1,
            exportedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            source = "android",
            storages = storages.map { ExportFormat.StorageExport(id = it.id.toInt(), name = it.name) },
            categories = categories.map { ExportFormat.CategoryExport(name = it.name) },
            movies = movies.map { m ->
                val filesData = getFilesByMovieId(m.id).map { fws ->
                    ExportFormat.FileExport(
                        name = fws.file.name,
                        size = fws.file.size,
                        storageNames = fws.storageNames
                    )
                }
                val categoryNames = getCategoriesByMovieId(m.id).map { it.name }
                ExportFormat.MovieExport(
                    tmdbId = m.tmdbId.toInt(),
                    mediaType = m.mediaType,
                    title = m.title,
                    originalTitle = m.originalTitle,
                    genres = m.genres,
                    rating = m.rating,
                    overview = m.overview,
                    releaseDate = m.releaseDate,
                    posterPath = m.posterPath,
                    personalRating = m.personalRating,
                    categoryNames = categoryNames.ifEmpty { null },
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
        for (c in data.categories.orEmpty()) {
            if (categoryDao.getByName(c.name) == null) {
                categoryDao.insert(CategoryEntity(name = c.name))
            }
        }

        var added = 0
        var skipped = 0
        for (m in data.movies) {
            val mediaType = m.mediaType ?: "movie"
            val exists = movieDao.existsByTmdbIdAndMediaType(m.tmdbId.toLong(), mediaType)
            val filesData = m.files ?: emptyList()

            if (exists) {
                if (replaceDuplicates) {
                    val existing = movieDao.getByTmdbIdAndMediaType(m.tmdbId.toLong(), mediaType)
                    if (existing != null) {
                        val updated = existing.copy(
                            title = m.title,
                            originalTitle = m.originalTitle,
                            genres = m.genres,
                            rating = m.rating,
                            overview = m.overview,
                            releaseDate = m.releaseDate,
                            posterPath = m.posterPath,
                            personalRating = m.personalRating,
                            mediaType = mediaType
                        )
                        movieDao.update(updated)
                        fileDao.deleteByMovieId(existing.id)
                        movieCategoryDao.deleteByMovieId(existing.id)
                        for (f in filesData) {
                            val fileId = fileDao.insert(FileEntity(name = f.name, size = f.size.toLong(), movieId = existing.id))
                            for (sn in f.storageNames.orEmpty()) {
                                storageDao.getByName(sn)?.let { storage ->
                                    storageFileDao.insert(StorageFileEntity(fileId = fileId, storageId = storage.id))
                                }
                            }
                        }
                        for (cn in m.categoryNames.orEmpty()) {
                            categoryDao.getByName(cn)?.let { cat ->
                                movieCategoryDao.insert(MovieCategoryEntity(movieId = existing.id, categoryId = cat.id))
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
                    mediaType = mediaType,
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
            for (cn in m.categoryNames.orEmpty()) {
                categoryDao.getByName(cn)?.let { cat ->
                    movieCategoryDao.insert(MovieCategoryEntity(movieId = id, categoryId = cat.id))
                }
            }
            added++
        }
        return Pair(added, skipped)
    }
}
