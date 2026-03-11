package com.sh.video.videolibrary.data.repository

import android.content.Context
import android.os.Build
import com.sh.video.videolibrary.util.LocaleHelper
import com.google.gson.Gson
import com.sh.video.videolibrary.data.local.ActorDao
import com.sh.video.videolibrary.data.local.ActorEntity
import com.sh.video.videolibrary.data.local.AppDatabase
import com.sh.video.videolibrary.data.local.GenreDao
import com.sh.video.videolibrary.data.local.GenreEntity
import com.sh.video.videolibrary.data.local.MovieGenreDao
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
import com.sh.video.videolibrary.data.remote.TmdbGenre
import com.sh.video.videolibrary.data.remote.TmdbMediaDetails
import com.sh.video.videolibrary.data.remote.TmdbMovieDetails
import com.sh.video.videolibrary.data.remote.TmdbTvDetails
import com.sh.video.videolibrary.export.ExportFormat
import com.sh.video.videolibrary.util.ArticleParseResult
import com.sh.video.videolibrary.util.ArticleParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class FileWithStorages(val file: FileEntity, val storageNames: List<String>)

data class StorageWithFileCount(val storage: StorageEntity, val fileCount: Int)

data class CategoryWithMovieCount(val category: CategoryEntity, val movieCount: Int)

data class ActorWithMovieCount(val actor: ActorEntity, val movieCount: Int)

data class GenreWithMovieCount(val genre: GenreEntity, val movieCount: Int)

class MovieRepository(
    private val context: Context,
    private val tmdbApiProvider: () -> TmdbApi
) {
    private val tmdbApi: TmdbApi get() = tmdbApiProvider()

    private val db: AppDatabase = DatabaseProvider.getDatabase(context)
    private val movieDao: MovieDao = db.movieDao()
    private val storageDao: StorageDao = db.storageDao()
    private val fileDao: FileDao = db.fileDao()
    private val storageFileDao: StorageFileDao = db.storageFileDao()
    private val categoryDao: CategoryDao = db.categoryDao()
    private val movieCategoryDao: MovieCategoryDao = db.movieCategoryDao()
    private val actorDao: ActorDao = db.actorDao()
    private val movieActorDao: MovieActorDao = db.movieActorDao()
    private val genreDao: GenreDao = db.genreDao()
    private val movieGenreDao: MovieGenreDao = db.movieGenreDao()
    private val gson = Gson()

    private fun getTmdbLanguage(): String {
        return when (LocaleHelper.getStoredLanguage(context)) {
            LocaleHelper.LANG_EN -> "en-US"
            LocaleHelper.LANG_RU -> "ru-RU"
            else -> {
                val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.resources.configuration.locales[0]
                } else {
                    @Suppress("DEPRECATION")
                    context.resources.configuration.locale
                }
                val tag = if (locale.country.isNotEmpty()) {
                    "${locale.language}-${locale.country.uppercase()}"
                } else {
                    locale.language
                }
                tag.ifEmpty { "en-US" }
            }
        }
    }

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

    suspend fun searchTmdbMovies(query: String) =
        tmdbApi.searchMovies(query = query, language = getTmdbLanguage())

    suspend fun searchTmdbTv(query: String) =
        tmdbApi.searchTv(query = query, language = getTmdbLanguage())

    /** Парсит статью по URL: названия фильмов и заголовок страницы для категории. */
    suspend fun parseArticle(url: String): Result<ArticleParseResult> = withContext(Dispatchers.IO) {
        ArticleParser.parseArticle(url)
    }

    suspend fun getTmdbMovieDetails(movieId: Long) =
        tmdbApi.getMovieDetails(movieId, language = getTmdbLanguage())

    suspend fun getTmdbTvDetails(tvId: Long) =
        tmdbApi.getTvDetails(tvId, language = getTmdbLanguage())

    suspend fun getMovieById(id: Long): MovieEntity? = movieDao.getById(id)

    suspend fun getMovieByTmdbId(tmdbId: Long, mediaType: String = "movie"): MovieEntity? =
        movieDao.getByTmdbIdAndMediaType(tmdbId, mediaType)

    private suspend fun ensureActorsForMovie(movieId: Long, tmdbId: Long, mediaType: String) {
        val lang = getTmdbLanguage()
        val credits = runCatching {
            when (mediaType) {
                "movie" -> tmdbApi.getMovieCredits(tmdbId, language = lang)
                else -> tmdbApi.getTvCredits(tmdbId, language = lang)
            }
        }.getOrNull() ?: return
        val cast = credits.cast ?: return
        val actorLinks = mutableListOf<Pair<Long, Int>>()
        for (member in cast) {
            if (member.billingOrder >= 10) continue // Загружаем только первых 10 в титрах
            val existingActorId = actorDao.getIdByTmdbPersonId(member.id)
            val actorId = run {
                val rowId = actorDao.insert(ActorEntity(tmdbPersonId = member.id, name = member.name))
                if (rowId == -1L) existingActorId else rowId
            }
            if (actorId != null && actorId > 0) {
                actorLinks.add(actorId to member.billingOrder)
            }
        }
        if (actorLinks.isNotEmpty()) {
            movieActorDao.setMovieActors(movieId, actorLinks)
        }
    }

    private suspend fun ensureGenresForMovie(movieId: Long, tmdbGenres: List<TmdbGenre>?) {
        if (tmdbGenres.isNullOrEmpty()) return
        val genreIds = tmdbGenres.mapNotNull { tg ->
            val existingId = genreDao.getIdByTmdbGenreId(tg.id)
            val id = existingId ?: run {
                val rowId = genreDao.insert(GenreEntity(tmdbGenreId = tg.id, name = tg.name))
                if (rowId == -1L) genreDao.getIdByTmdbGenreId(tg.id) else rowId
            }
            id?.takeIf { it > 0 }
        }.distinct()
        if (genreIds.isNotEmpty()) movieGenreDao.setMovieGenres(movieId, genreIds)
    }

    /** Парсит строку жанров и связывает фильм с таблицей жанров (для импорта). */
    private suspend fun ensureGenresFromString(movieId: Long, genresStr: String) {
        if (genresStr.isBlank()) return
        val names = genresStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val genreIds = names.mapNotNull { name ->
            val existing = genreDao.getByName(name)
            if (existing != null) existing.id
            else {
                val rowId = genreDao.insert(GenreEntity(tmdbGenreId = null, name = name))
                if (rowId == -1L) genreDao.getByName(name)?.id else rowId
            }
        }
        if (genreIds.isNotEmpty()) movieGenreDao.setMovieGenres(movieId, genreIds)
    }

    /** Возвращает имёна первых 10 актёров из credits TMDB (для превью до добавления в базу). */
    suspend fun getTmdbCreditsTopActorNames(tmdbId: Long, mediaType: String): List<String> {
        val lang = getTmdbLanguage()
        val credits = runCatching {
            when (mediaType) {
                "movie" -> tmdbApi.getMovieCredits(tmdbId, language = lang)
                else -> tmdbApi.getTvCredits(tmdbId, language = lang)
            }
        }.getOrNull() ?: return emptyList()
        return (credits.cast ?: emptyList())
            .filter { it.billingOrder < 10 }
            .sortedBy { it.billingOrder }
            .take(10)
            .map { it.name }
    }

    /** Загружает актёров с TMDB для существующего фильма, если в базе их ещё нет. */
    suspend fun loadActorsFromTmdbIfEmpty(movieId: Long, tmdbId: Long, mediaType: String) {
        val existingIds = movieActorDao.getActorIdsByMovieId(movieId)
        if (existingIds.isEmpty()) {
            ensureActorsForMovie(movieId, tmdbId, mediaType)
        }
    }

    /** Обновляет имена актёров на русские по данным TMDB (person translations). Возвращает количество обновлённых. */
    suspend fun refreshActorNamesToRussian(): Int {
        val actors = actorDao.getAllSync()
        var updated = 0
        for (actor in actors) {
            val translations = runCatching { tmdbApi.getPersonTranslations(actor.tmdbPersonId) }.getOrNull()
            val ruName = translations?.translations
                ?.firstOrNull { it.iso6391 == "ru" || it.iso31661 == "RU" }
                ?.data?.name
                ?.takeIf { it.isNotBlank() }
            if (ruName != null && ruName != actor.name) {
                actorDao.update(actor.copy(name = ruName))
                updated++
            }
        }
        return updated
    }

    /** Обновляет данные фильма/сериала с TMDb (название, описание, жанры на текущем языке). Сохраняет personalRating. */
    suspend fun refreshMovieFromTmdb(movieId: Long): Boolean {
        val existing = movieDao.getById(movieId) ?: return false
        val lang = getTmdbLanguage()
        var genresToSync: List<TmdbGenre>? = null
        val updated = when (existing.mediaType) {
            "movie" -> {
                val d = runCatching { tmdbApi.getMovieDetails(existing.tmdbId, language = lang) }.getOrNull() ?: return false
                genresToSync = d.genres
                existing.copy(
                    title = d.title,
                    originalTitle = d.originalTitle ?: "",
                    genres = d.genres?.joinToString(", ") { it.name } ?: existing.genres,
                    rating = d.voteAverage ?: existing.rating,
                    overview = d.overview ?: "",
                    releaseDate = d.releaseDate ?: "",
                    posterPath = d.posterPath
                )
            }
            "tv" -> {
                val d = runCatching { tmdbApi.getTvDetails(existing.tmdbId, language = lang) }.getOrNull() ?: return false
                genresToSync = d.genres
                existing.copy(
                    title = d.name,
                    originalTitle = d.originalName ?: "",
                    genres = d.genres?.joinToString(", ") { it.name } ?: existing.genres,
                    rating = d.voteAverage ?: existing.rating,
                    overview = d.overview ?: "",
                    releaseDate = d.firstAirDate ?: "",
                    posterPath = d.posterPath
                )
            }
            else -> return false
        }
        movieDao.update(updated)
        ensureActorsForMovie(movieId, existing.tmdbId, existing.mediaType)
        genresToSync?.let { ensureGenresForMovie(movieId, it) }
        return true
    }

    /** Обновляет актёров с TMDB для всей коллекции. Возвращает количество обработанных фильмов. */
    suspend fun refreshActorsForAllMovies(): Int {
        val movies = movieDao.getAllSync()
        for (movie in movies) {
            ensureActorsForMovie(movie.id, movie.tmdbId, movie.mediaType)
        }
        return movies.size
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
            ensureGenresForMovie(id, details.genres)
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
            ensureGenresForMovie(id, details.genres)
        }
        return id
    }

    suspend fun addMedia(details: TmdbMediaDetails): Long = when (details) {
        is TmdbMediaDetails.Movie -> addMovie(details.data)
        is TmdbMediaDetails.Tv -> addTvShow(details.data)
    }

    /**
     * Добавляет фильм/сериал в коллекцию (если ещё нет) и привязывает к категории.
     * Если уже есть — только добавляет категорию.
     * @return id фильма в базе или -1 при ошибке
     */
    suspend fun addMediaWithCategory(details: TmdbMediaDetails, categoryId: Long): Long {
        val id = addMedia(details)
        val finalId = when {
            id > 0 -> id
            else -> getMovieByTmdbId(details.id, details.mediaType)?.id ?: return -1
        }
        val currentIds = getCategoryIdsByMovieId(finalId)
        if (categoryId !in currentIds) {
            setMovieCategories(finalId, currentIds + categoryId)
        }
        return finalId
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

    fun getGenresWithMovieCounts(): Flow<List<GenreWithMovieCount>> = flow {
        genreDao.getAllFlow().collect { genres ->
            val withCounts = genres.map { g ->
                GenreWithMovieCount(g, movieGenreDao.getMovieCountByGenreId(g.id))
            }.filter { it.movieCount > 0 }
            emit(withCounts)
        }
    }

    suspend fun getMoviesByGenreId(genreId: Long): List<MovieEntity> =
        movieGenreDao.getMoviesByGenreId(genreId)

    suspend fun getGenreById(id: Long): GenreEntity? = genreDao.getById(id)

    suspend fun getGenresByMovieId(movieId: Long): List<GenreEntity> =
        movieGenreDao.getGenresByMovieId(movieId)

    suspend fun getGenresForMovies(movieIds: List<Long>): Map<Long, List<GenreEntity>> {
        if (movieIds.isEmpty()) return emptyMap()
        return movieIds.associateWith { movieGenreDao.getGenresByMovieId(it) }
    }

    suspend fun getCategoryCountByMovieIds(movieIds: List<Long>): Map<Long, Int> {
        if (movieIds.isEmpty()) return emptyMap()
        return movieIds.associateWith { movieCategoryDao.getCategoryIdsByMovieId(it).size }
    }

    /** Возвращает до limit актёров в главных ролях для фильма (по порядку в титрах). */
    suspend fun getTopActorsByMovieId(movieId: Long, limit: Int = 5): List<ActorEntity> {
        val rows = movieActorDao.getActorsForMovies(listOf(movieId))
        return rows
            .filter { it.movieId == movieId }
            .take(limit)
            .map { ActorEntity(id = it.actorId, tmdbPersonId = it.tmdbPersonId, name = it.actorName) }
    }

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
                        ensureGenresFromString(existing.id, m.genres)
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
            ensureGenresFromString(id, m.genres)
            added++
        }
        return Pair(added, skipped)
    }
}
