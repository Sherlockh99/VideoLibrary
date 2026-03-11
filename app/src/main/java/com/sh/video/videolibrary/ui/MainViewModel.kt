package com.sh.video.videolibrary.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sh.video.videolibrary.R
import com.sh.video.videolibrary.VideoLibraryApp
import com.sh.video.videolibrary.data.local.FileOnStorageRow
import com.sh.video.videolibrary.data.repository.ActorWithMovieCount
import com.sh.video.videolibrary.data.repository.CategoryWithMovieCount
import com.sh.video.videolibrary.data.repository.GenreWithMovieCount
import com.sh.video.videolibrary.data.repository.FileWithStorages
import com.sh.video.videolibrary.data.repository.StorageWithFileCount
import com.sh.video.videolibrary.data.local.ActorEntity
import com.sh.video.videolibrary.data.local.CategoryEntity
import com.sh.video.videolibrary.data.local.GenreEntity
import com.sh.video.videolibrary.ui.MovieWithActorsAndGenres
import com.sh.video.videolibrary.data.local.MovieEntity
import com.sh.video.videolibrary.data.local.StorageEntity
import com.sh.video.videolibrary.data.remote.TmdbMediaDetails
import com.sh.video.videolibrary.data.remote.TmdbMovieDetails
import com.sh.video.videolibrary.data.repository.MovieRepository
import com.sh.video.videolibrary.util.ArticleParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import java.io.File

enum class LibraryFilter {
    ALL,
    BY_TITLE,
    BY_GENRE,
    BY_TMDB_RATING,
    BY_PERSONAL_RATING,
    BY_STORAGE
}

enum class StorageFileFilter {
    ALL,
    BY_MOVIE_TITLE,
    BY_GENRE,
    BY_TMDB_RATING,
    BY_PERSONAL_RATING,
    BY_FILE_NAME
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(context: Context) : ViewModel() {

    private val app = context.applicationContext as VideoLibraryApp
    private val repository = MovieRepository(context) { app.tmdbApi }

    private val _libraryFilter = MutableStateFlow(LibraryFilter.ALL)
    val libraryFilter = _libraryFilter.asStateFlow()

    private val _filterQuery = MutableStateFlow("")
    val filterQuery = _filterQuery.asStateFlow()

    private val _filterPersonalRating = MutableStateFlow<Int?>(null)
    val filterPersonalRating = _filterPersonalRating.asStateFlow()

    private val _filterTmdbMinRating = MutableStateFlow<Double?>(null)
    val filterTmdbMinRating = _filterTmdbMinRating.asStateFlow()

    val library: StateFlow<List<MovieEntity>> = combine(
        _libraryFilter,
        _filterQuery,
        _filterPersonalRating,
        _filterTmdbMinRating
    ) { filter, query, personalRating, tmdbMin ->
        Triple(filter, query, Pair(personalRating, tmdbMin))
    }.flatMapLatest { (filter, query, ratings) ->
        val (personalRating, tmdbMin) = ratings
        when (filter) {
            LibraryFilter.ALL -> repository.getAllMovies()
            LibraryFilter.BY_TITLE -> repository.searchMovies(
                title = query.ifBlank { null },
                genre = null,
                minRating = null,
                maxRating = null,
                description = null,
                personalRating = null,
                storageName = null
            )
            LibraryFilter.BY_GENRE -> repository.searchMovies(
                title = null,
                genre = query.ifBlank { null },
                minRating = null,
                maxRating = null,
                description = null,
                personalRating = null,
                storageName = null
            )
            LibraryFilter.BY_TMDB_RATING -> repository.searchMovies(
                title = null,
                genre = null,
                minRating = tmdbMin,
                maxRating = null,
                description = null,
                personalRating = null,
                storageName = null
            )
            LibraryFilter.BY_PERSONAL_RATING -> repository.searchMovies(
                title = null,
                genre = null,
                minRating = null,
                maxRating = null,
                description = null,
                personalRating = personalRating,
                storageName = null
            )
            LibraryFilter.BY_STORAGE -> repository.searchMovies(
                title = null,
                genre = null,
                minRating = null,
                maxRating = null,
                description = null,
                personalRating = null,
                storageName = query.ifBlank { null }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Коллекция с актёрами, жанрами и количеством категорий для отображения на карточках. */
    val libraryWithTopActors: StateFlow<List<MovieWithActorsAndGenres>> = library
        .flatMapLatest { movies ->
            flow {
                val actorMap = repository.getTopActorNamesByMovieIds(movies.map { it.id }, limit = 5)
                val genreMap = repository.getGenresForMovies(movies.map { it.id })
                val categoryCountMap = repository.getCategoryCountByMovieIds(movies.map { it.id })
                emit(movies.map { movie ->
                    MovieWithActorsAndGenres(
                        movie = movie,
                        topActorNames = actorMap[movie.id] ?: emptyList(),
                        genres = genreMap[movie.id] ?: emptyList(),
                        categoryCount = categoryCountMap[movie.id] ?: 0
                    )
                })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storages: StateFlow<List<StorageEntity>> = repository.getAllStorages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storagesWithFileCounts: StateFlow<List<StorageWithFileCount>> =
        repository.getStoragesWithFileCounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoriesWithMovieCounts: StateFlow<List<CategoryWithMovieCount>> =
        repository.getCategoriesWithMovieCounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> =
        repository.getAllCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val actorsWithMovieCounts: StateFlow<List<ActorWithMovieCount>> =
        repository.getActorsWithMovieCounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val genresWithMovieCounts: StateFlow<List<GenreWithMovieCount>> =
        repository.getGenresWithMovieCounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _categoryRemoveError = MutableStateFlow<String?>(null)
    val categoryRemoveError = _categoryRemoveError.asStateFlow()

    private val _storageRemoveError = MutableStateFlow<String?>(null)
    val storageRemoveError = _storageRemoveError.asStateFlow()

    private val _storageFiles = MutableStateFlow<List<FileOnStorageRow>>(emptyList())
    val storageFiles = _storageFiles.asStateFlow()

    private val _storageFileFilter = MutableStateFlow(StorageFileFilter.ALL)
    val storageFileFilter = _storageFileFilter.asStateFlow()

    private val _storageFileFilterQuery = MutableStateFlow("")
    val storageFileFilterQuery = _storageFileFilterQuery.asStateFlow()

    private val _storageFileFilterTmdbMinRating = MutableStateFlow<Double?>(null)
    val storageFileFilterTmdbMinRating = _storageFileFilterTmdbMinRating.asStateFlow()

    private val _storageFileFilterPersonalRating = MutableStateFlow<Int?>(null)
    val storageFileFilterPersonalRating = _storageFileFilterPersonalRating.asStateFlow()

    val filteredStorageFiles = combine(
        _storageFiles,
        _storageFileFilter,
        _storageFileFilterQuery,
        _storageFileFilterTmdbMinRating,
        _storageFileFilterPersonalRating
    ) { files, filter, query, tmdbMin, personalRating ->
        when (filter) {
            StorageFileFilter.ALL -> files
            StorageFileFilter.BY_MOVIE_TITLE -> {
                val q = query.trim().lowercase()
                if (q.isEmpty()) files else files.filter { it.movieTitle.lowercase().contains(q) }
            }
            StorageFileFilter.BY_GENRE -> {
                val q = query.trim().lowercase()
                if (q.isEmpty()) files else files.filter { it.genres.lowercase().contains(q) }
            }
            StorageFileFilter.BY_TMDB_RATING -> {
                if (tmdbMin == null) files else files.filter { it.rating >= tmdbMin }
            }
            StorageFileFilter.BY_PERSONAL_RATING -> {
                if (personalRating == null) files else files.filter { it.personalRating == personalRating }
            }
            StorageFileFilter.BY_FILE_NAME -> {
                val q = query.trim().lowercase()
                if (q.isEmpty()) files else files.filter { it.fileName.lowercase().contains(q) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setStorageFileFilter(filter: StorageFileFilter) {
        _storageFileFilter.value = filter
        if (filter == StorageFileFilter.ALL) {
            _storageFileFilterQuery.value = ""
            _storageFileFilterTmdbMinRating.value = null
            _storageFileFilterPersonalRating.value = null
        }
    }

    fun setStorageFileFilterTmdbMinRating(rating: Double?) {
        _storageFileFilterTmdbMinRating.value = rating
    }

    fun setStorageFileFilterPersonalRating(rating: Int?) {
        _storageFileFilterPersonalRating.value = rating
    }

    fun setStorageFileFilterQuery(query: String) {
        _storageFileFilterQuery.value = query
    }

    fun loadStorageFiles(storageId: Long) {
        viewModelScope.launch {
            _storageFiles.value = repository.getFilesByStorageId(storageId)
            _storageFileFilter.value = StorageFileFilter.ALL
            _storageFileFilterQuery.value = ""
            _storageFileFilterTmdbMinRating.value = null
            _storageFileFilterPersonalRating.value = null
        }
    }

    fun removeFileFromStorage(fileId: Long, storageId: Long) {
        viewModelScope.launch {
            repository.removeFileFromStorage(fileId, storageId)
            _storageFiles.value = repository.getFilesByStorageId(storageId)
        }
    }

    suspend fun getMovieByTmdbId(tmdbId: Long, mediaType: String = "movie"): MovieEntity? =
        repository.getMovieByTmdbId(tmdbId, mediaType)

    /** Добавляет файл в хранилище. Создаёт фильм/сериал если нужно, создаёт файл, привязывает к storageId. */
    suspend fun addFileToStorage(
        storageId: Long,
        movieId: Long?,
        tmdbDetails: TmdbMediaDetails?,
        fileName: String,
        fileSize: Long
    ): Result<Unit> = runCatching {
        val finalMovieId = when {
            movieId != null -> movieId
            tmdbDetails != null -> {
                var id = repository.getMovieByTmdbId(tmdbDetails.id, tmdbDetails.mediaType)?.id
                if (id == null) {
                    val added = repository.addMedia(tmdbDetails)
                    id = if (added == -1L) repository.getMovieByTmdbId(tmdbDetails.id, tmdbDetails.mediaType)?.id else added
                    if (id == null) throw IllegalArgumentException("Не удалось добавить в коллекцию")
                }
                id!!
            }
            else -> throw IllegalArgumentException("Выберите из коллекции или добавьте из TMDb")
        }
        val fileId = repository.addFile(finalMovieId, fileName, fileSize)
        repository.addFileToStorage(fileId, storageId)
        _storageFiles.value = repository.getFilesByStorageId(storageId)
    }

    fun clearStorageRemoveError() {
        _storageRemoveError.value = null
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.addCategory(name)
        }
    }

    fun addCategoryAndLinkToMovie(movieId: Long, name: String) {
        viewModelScope.launch {
            val categoryId = repository.addCategory(name.trim())
            val currentIds = repository.getCategoryIdsByMovieId(movieId)
            if (categoryId !in currentIds) {
                repository.setMovieCategories(movieId, currentIds + categoryId)
                _movieCategories.value = repository.getCategoriesByMovieId(movieId)
            }
        }
    }

    fun updateCategory(id: Long, name: String) {
        viewModelScope.launch {
            repository.updateCategory(id, name)
        }
    }

    fun removeCategory(id: Long) {
        viewModelScope.launch {
            _categoryRemoveError.value = null
            if (!repository.removeCategory(id)) {
                _categoryRemoveError.value = app.getString(R.string.error_category_has_movies)
            }
        }
    }

    fun clearCategoryRemoveError() {
        _categoryRemoveError.value = null
    }

    private val _movieCategories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val movieCategories = _movieCategories.asStateFlow()

    private val _categoryMovies = MutableStateFlow<List<MovieWithActorsAndGenres>>(emptyList())
    val categoryMovies = _categoryMovies.asStateFlow()

    private var _pendingCategoryIdForNewMovie: Long? = null
    private var _pendingActorIdForNewMovie: Long? = null
    private var _currentCategoryId: Long = 0
    private var _currentActorId: Long = 0

    private val _returnToCategoryIdAfterAdd = MutableStateFlow<Long?>(null)
    val returnToCategoryIdAfterAdd = _returnToCategoryIdAfterAdd.asStateFlow()

    fun loadMovieCategories(movieId: Long) {
        viewModelScope.launch {
            _movieCategories.value = repository.getCategoriesByMovieId(movieId)
        }
    }

    fun setMovieCategories(movieId: Long, categoryIds: List<Long>) {
        viewModelScope.launch {
            repository.setMovieCategories(movieId, categoryIds)
            _movieCategories.value = repository.getCategoriesByMovieId(movieId)
        }
    }

    fun addMovieToCategory(movieId: Long, categoryId: Long) {
        viewModelScope.launch {
            val currentIds = repository.getCategoryIdsByMovieId(movieId)
            if (categoryId !in currentIds) {
                repository.setMovieCategories(movieId, currentIds + categoryId)
            }
            if (categoryId == _currentCategoryId) {
                loadCategoryMoviesWithActors(categoryId)
            }
        }
    }

    fun removeMovieFromCategory(movieId: Long, categoryId: Long) {
        viewModelScope.launch {
            val currentIds = repository.getCategoryIdsByMovieId(movieId)
            repository.setMovieCategories(movieId, currentIds.filter { it != categoryId })
            if (categoryId == _currentCategoryId) {
                loadCategoryMoviesWithActors(categoryId)
            }
        }
    }

    fun loadCategoryMovies(categoryId: Long) {
        _currentCategoryId = categoryId
        viewModelScope.launch {
            loadCategoryMoviesWithActors(categoryId)
        }
    }

    private suspend fun loadCategoryMoviesWithActors(categoryId: Long) {
        val movies = repository.getMoviesByCategoryId(categoryId)
        val actorMap = repository.getTopActorNamesByMovieIds(movies.map { it.id }, limit = 5)
        val genreMap = repository.getGenresForMovies(movies.map { it.id })
        _categoryMovies.value = movies.map { movie ->
            MovieWithActorsAndGenres(movie, actorMap[movie.id] ?: emptyList(), genreMap[movie.id] ?: emptyList())
        }
    }

    private val _actorMovies = MutableStateFlow<List<MovieWithActorsAndGenres>>(emptyList())
    val actorMovies = _actorMovies.asStateFlow()

    fun loadActorMovies(actorId: Long) {
        _currentActorId = actorId
        viewModelScope.launch {
            val movies = repository.getMoviesByActorId(actorId)
            val actorMap = repository.getTopActorNamesByMovieIds(movies.map { it.id }, limit = 5)
            val genreMap = repository.getGenresForMovies(movies.map { it.id })
            _actorMovies.value = movies.map { movie ->
                MovieWithActorsAndGenres(movie, actorMap[movie.id] ?: emptyList(), genreMap[movie.id] ?: emptyList())
            }
        }
    }

    fun setPendingActorIdForNewMovie(actorId: Long) {
        _pendingActorIdForNewMovie = actorId
    }

    fun clearPendingActorIdForNewMovie() {
        _pendingActorIdForNewMovie = null
    }

    private val _selectedActor = MutableStateFlow<ActorEntity?>(null)
    val selectedActor = _selectedActor.asStateFlow()

    private val _genreMovies = MutableStateFlow<List<MovieWithActorsAndGenres>>(emptyList())
    val genreMovies = _genreMovies.asStateFlow()

    private val _selectedGenre = MutableStateFlow<GenreEntity?>(null)
    val selectedGenre = _selectedGenre.asStateFlow()

    fun loadGenreMovies(genreId: Long) {
        viewModelScope.launch {
            val movies = repository.getMoviesByGenreId(genreId)
            val actorMap = repository.getTopActorNamesByMovieIds(movies.map { it.id }, limit = 5)
            val genreMap = repository.getGenresForMovies(movies.map { it.id })
            _genreMovies.value = movies.map { movie ->
                MovieWithActorsAndGenres(movie, actorMap[movie.id] ?: emptyList(), genreMap[movie.id] ?: emptyList())
            }
        }
    }

    fun loadGenre(genreId: Long) {
        viewModelScope.launch {
            _selectedGenre.value = repository.getGenreById(genreId)
            _selectedGenre.value?.let { loadGenreMovies(it.id) }
        }
    }

    fun loadActor(actorId: Long) {
        viewModelScope.launch {
            _selectedActor.value = repository.getActorById(actorId)
            _selectedActor.value?.let { loadActorMovies(it.id) }
        }
    }

    fun setPendingCategoryForNewMovie(categoryId: Long) {
        _pendingCategoryIdForNewMovie = categoryId
    }

    fun clearPendingCategoryForNewMovie() {
        _pendingCategoryIdForNewMovie = null
    }

    fun clearReturnToCategoryIdAfterAdd() {
        _returnToCategoryIdAfterAdd.value = null
    }

    private val _searchResults = MutableStateFlow<List<TmdbMediaDetails>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _searchMode = MutableStateFlow(SearchMode.MOVIE)
    val searchMode = _searchMode.asStateFlow()

    private val _selectedTmdbForPreview = MutableStateFlow<TmdbMediaDetails?>(null)
    val selectedTmdbForPreview = _selectedTmdbForPreview.asStateFlow()

    private val _tmdbPreviewActors = MutableStateFlow<List<String>>(emptyList())
    val tmdbPreviewActors = _tmdbPreviewActors.asStateFlow()

    enum class SearchMode { MOVIE, TV }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _exportResult = MutableStateFlow<ExportResult?>(null)
    val exportResult = _exportResult.asStateFlow()

    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult = _importResult.asStateFlow()

    private val _actorsRefreshResult = MutableStateFlow<ActorsRefreshResult?>(null)
    val actorsRefreshResult = _actorsRefreshResult.asStateFlow()

    private val _actorsRefreshInProgress = MutableStateFlow(false)
    val actorsRefreshInProgress = _actorsRefreshInProgress.asStateFlow()

    private val _actorNamesRefreshResult = MutableStateFlow<ActorNamesRefreshResult?>(null)
    val actorNamesRefreshResult = _actorNamesRefreshResult.asStateFlow()

    private val _actorNamesRefreshInProgress = MutableStateFlow(false)
    val actorNamesRefreshInProgress = _actorNamesRefreshInProgress.asStateFlow()

    private val _selectedMovie = MutableStateFlow<MovieEntity?>(null)
    val selectedMovie = _selectedMovie.asStateFlow()

    private val _movieFiles = MutableStateFlow<List<FileWithStorages>>(emptyList())
    val movieFiles = _movieFiles.asStateFlow()

    private val _movieTopActors = MutableStateFlow<List<ActorEntity>>(emptyList())
    val movieTopActors = _movieTopActors.asStateFlow()

    private val _movieGenres = MutableStateFlow<List<GenreEntity>>(emptyList())
    val movieGenres = _movieGenres.asStateFlow()

    private val _movieFilesForAddDialog = MutableStateFlow<List<FileWithStorages>>(emptyList())
    val movieFilesForAddDialog = _movieFilesForAddDialog.asStateFlow()

    fun loadMovieFilesForAddDialog(movieId: Long) {
        viewModelScope.launch {
            _movieFilesForAddDialog.value = if (movieId > 0) repository.getFilesByMovieId(movieId) else emptyList()
        }
    }

    suspend fun linkExistingFilesToStorage(fileIds: List<Long>, storageId: Long): Result<Unit> =
        runCatching {
            fileIds.forEach { repository.addFileToStorage(it, storageId) }
            _storageFiles.value = repository.getFilesByStorageId(storageId)
        }

    fun selectMovie(movie: MovieEntity) {
        _selectedMovie.value = movie
        viewModelScope.launch {
            _movieFiles.value = repository.getFilesByMovieId(movie.id)
            _movieCategories.value = repository.getCategoriesByMovieId(movie.id)
            repository.loadActorsFromTmdbIfEmpty(movie.id, movie.tmdbId, movie.mediaType)
            _movieTopActors.value = repository.getTopActorsByMovieId(movie.id, limit = 5)
            _movieGenres.value = repository.getGenresByMovieId(movie.id)
        }
    }

    fun selectMovieById(movieId: Long, onSelected: (() -> Unit)? = null) {
        viewModelScope.launch {
            val movie = repository.getMovieById(movieId)
            if (movie != null) {
                _selectedMovie.value = movie
                _movieFiles.value = repository.getFilesByMovieId(movieId)
                _movieCategories.value = repository.getCategoriesByMovieId(movieId)
                repository.loadActorsFromTmdbIfEmpty(movie.id, movie.tmdbId, movie.mediaType)
                _movieTopActors.value = repository.getTopActorsByMovieId(movieId, limit = 5)
                _movieGenres.value = repository.getGenresByMovieId(movieId)
                onSelected?.invoke()
            }
        }
    }

    fun clearSelectedMovie() {
        _selectedMovie.value = null
        _movieFiles.value = emptyList()
        _movieCategories.value = emptyList()
        _movieTopActors.value = emptyList()
        _movieGenres.value = emptyList()
    }

    private val _movieRefreshInProgress = MutableStateFlow(false)
    val movieRefreshInProgress = _movieRefreshInProgress.asStateFlow()

    fun refreshMovieFromTmdb(movieId: Long) {
        viewModelScope.launch {
            _movieRefreshInProgress.value = true
            try {
                if (repository.refreshMovieFromTmdb(movieId)) {
                    val updated = repository.getMovieById(movieId)
                    if (updated != null && _selectedMovie.value?.id == movieId) {
                        _selectedMovie.value = updated
                        _movieTopActors.value = repository.getTopActorsByMovieId(movieId, limit = 5)
                        _movieGenres.value = repository.getGenresByMovieId(movieId)
                    }
                }
            } finally {
                _movieRefreshInProgress.value = false
            }
        }
    }

    fun setLibraryFilter(filter: LibraryFilter) {
        _libraryFilter.value = filter
    }

    fun setFilterQuery(query: String) {
        _filterQuery.value = query
    }

    fun setFilterPersonalRating(rating: Int?) {
        _filterPersonalRating.value = rating
    }

    fun setFilterTmdbMinRating(rating: Double?) {
        _filterTmdbMinRating.value = rating
    }

    fun addStorage(name: String) {
        viewModelScope.launch {
            repository.addStorage(name)
        }
    }

    fun updateStorage(id: Long, name: String) {
        viewModelScope.launch {
            repository.updateStorage(id, name)
        }
    }

    fun removeStorage(id: Long) {
        viewModelScope.launch {
            _storageRemoveError.value = null
            if (!repository.removeStorage(id)) {
                _storageRemoveError.value = app.getString(R.string.error_storage_has_files)
            }
        }
    }

    fun addFile(movieId: Long, name: String, size: Long, storageIds: List<Long>) {
        viewModelScope.launch {
            val fileId = repository.addFile(movieId, name, size)
            storageIds.forEach { repository.addFileToStorage(fileId, it) }
            _selectedMovie.value?.let { m ->
                if (m.id == movieId) _movieFiles.value = repository.getFilesByMovieId(movieId)
            }
        }
    }

    fun updateFile(fileId: Long, name: String, size: Long, storageIds: List<Long>) {
        viewModelScope.launch {
            repository.updateFile(fileId, name, size, storageIds)
            _selectedMovie.value?.let { m ->
                _movieFiles.value = repository.getFilesByMovieId(m.id)
            }
        }
    }

    fun removeFile(fileId: Long) {
        viewModelScope.launch {
            repository.removeFile(fileId)
            _selectedMovie.value?.let { m ->
                _movieFiles.value = repository.getFilesByMovieId(m.id)
            }
        }
    }

    private val _movieDeleted = MutableStateFlow(false)
    val movieDeleted = _movieDeleted.asStateFlow()

    fun removeMovie(movieId: Long) {
        viewModelScope.launch {
            if (repository.removeMovie(movieId)) {
                _movieDeleted.value = true
            }
        }
    }

    fun clearMovieDeleted() {
        _movieDeleted.value = false
    }

    fun setSearchMode(mode: SearchMode) {
        _searchMode.value = mode
        _searchResults.value = emptyList()
    }

    fun searchTmdb(query: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val details = when (_searchMode.value) {
                    SearchMode.MOVIE -> {
                        val response = repository.searchTmdbMovies(query)
                        response.results.mapNotNull { r ->
                            runCatching { repository.getTmdbMovieDetails(r.id) }
                                .getOrNull()?.let { TmdbMediaDetails.Movie(it) }
                        }
                    }
                    SearchMode.TV -> {
                        val response = repository.searchTmdbTv(query)
                        response.results.mapNotNull { r ->
                            runCatching { repository.getTmdbTvDetails(r.id) }
                                .getOrNull()?.let { TmdbMediaDetails.Tv(it) }
                        }
                    }
                }
                _searchResults.value = details
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: app.getString(R.string.error_search))
            }
        }
    }

    fun selectTmdbForPreview(details: TmdbMediaDetails) {
        _selectedTmdbForPreview.value = details
        if (_uiState.value is UiState.Error) _uiState.value = UiState.Idle
    }

    fun clearTmdbPreview() {
        _selectedTmdbForPreview.value = null
        _tmdbPreviewActors.value = emptyList()
    }

    fun loadTmdbPreviewActors(tmdbId: Long, mediaType: String) {
        viewModelScope.launch {
            _tmdbPreviewActors.value = repository.getTmdbCreditsTopActorNames(tmdbId, mediaType)
        }
    }

    private val _movieAddedSuccess = MutableStateFlow(false)
    val movieAddedSuccess = _movieAddedSuccess.asStateFlow()

    private val _movieIdJustAdded = MutableStateFlow<Long?>(null)
    val movieIdJustAdded = _movieIdJustAdded.asStateFlow()

    private val _openDetailOnFilesTab = MutableStateFlow(false)
    val openDetailOnFilesTab = _openDetailOnFilesTab.asStateFlow()

    fun setOpenDetailOnFilesTab(value: Boolean) {
        _openDetailOnFilesTab.value = value
    }

    fun clearOpenDetailOnFilesTab() {
        _openDetailOnFilesTab.value = false
    }

    fun addMovie(details: TmdbMediaDetails) {
        viewModelScope.launch {
            val id = repository.addMedia(details)
            if (id == -1L) {
                _uiState.value = UiState.Error(
                    if (details.mediaType == "tv") app.getString(R.string.error_tv_already_in_collection) else app.getString(R.string.error_movie_already_in_collection)
                )
            } else {
                _pendingCategoryIdForNewMovie?.let { catId ->
                    repository.setMovieCategories(id, repository.getCategoryIdsByMovieId(id) + catId)
                    _returnToCategoryIdAfterAdd.value = catId
                    _pendingCategoryIdForNewMovie = null
                    if (catId == _currentCategoryId) {
                        loadCategoryMoviesWithActors(catId)
                    }
                }
                _pendingActorIdForNewMovie?.let { actorId ->
                    if (actorId == _currentActorId) {
                        val movies = repository.getMoviesByActorId(actorId)
                        val actorMap = repository.getTopActorNamesByMovieIds(movies.map { it.id }, limit = 5)
                        val genreMap = repository.getGenresForMovies(movies.map { it.id })
                        _actorMovies.value = movies.map { movie ->
                            MovieWithActorsAndGenres(movie, actorMap[movie.id] ?: emptyList(), genreMap[movie.id] ?: emptyList())
                        }
                    }
                    _pendingActorIdForNewMovie = null
                }
                _movieIdJustAdded.value = id
                _movieAddedSuccess.value = true
            }
        }
    }

    fun clearMovieAddedSuccess() {
        _movieAddedSuccess.value = false
        _movieIdJustAdded.value = null
    }

    fun updateRating(id: Long, rating: Int) {
        viewModelScope.launch {
            repository.updatePersonalRating(id, rating)
        }
    }

    fun exportToUri(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val out = context.contentResolver.openOutputStream(uri)
                    ?: run {
                        _exportResult.value = ExportResult.Failure("Не удалось открыть файл")
                        return@launch
                    }
                try {
                    val count = repository.exportToOutputStream(out)
                    _exportResult.value = ExportResult.Success(count)
                } finally {
                    out.close()
                }
            } catch (e: Exception) {
                _exportResult.value = ExportResult.Failure(e.message ?: "Ошибка")
            }
        }
    }

    fun importFromUri(uri: Uri, replaceDuplicates: Boolean) {
        viewModelScope.launch {
            try {
                val json = context.applicationContext.contentResolver
                    .openInputStream(uri)?.bufferedReader()?.readText()
                    ?: throw IllegalArgumentException("Не удалось прочитать файл")
                val (added, skipped) = repository.importFromFileContent(json, replaceDuplicates)
                _importResult.value = ImportResult.Success(added, skipped)
            } catch (e: Exception) {
                _importResult.value = ImportResult.Failure(e.message ?: "Ошибка")
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    fun clearUiStateError() {
        if (_uiState.value is UiState.Error) _uiState.value = UiState.Idle
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    fun clearImportResult() {
        _importResult.value = null
    }

    fun refreshActorsForAllMovies() {
        viewModelScope.launch {
            _actorsRefreshInProgress.value = true
            _actorsRefreshResult.value = null
            try {
                val count = repository.refreshActorsForAllMovies()
                _actorsRefreshResult.value = ActorsRefreshResult.Success(count)
            } catch (e: Exception) {
                _actorsRefreshResult.value = ActorsRefreshResult.Failure(e.message ?: "Ошибка")
            } finally {
                _actorsRefreshInProgress.value = false
            }
        }
    }

    fun clearActorsRefreshResult() {
        _actorsRefreshResult.value = null
    }

    fun refreshActorNamesToRussian() {
        viewModelScope.launch {
            _actorNamesRefreshInProgress.value = true
            _actorNamesRefreshResult.value = null
            try {
                val count = repository.refreshActorNamesToRussian()
                _actorNamesRefreshResult.value = ActorNamesRefreshResult.Success(count)
            } catch (e: Exception) {
                _actorNamesRefreshResult.value = ActorNamesRefreshResult.Failure(e.message ?: "Ошибка")
            } finally {
                _actorNamesRefreshInProgress.value = false
            }
        }
    }

    fun clearActorNamesRefreshResult() {
        _actorNamesRefreshResult.value = null
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }

    sealed class ExportResult {
        data class Success(val count: Int) : ExportResult()
        data class Failure(val message: String) : ExportResult()
    }

    sealed class ImportResult {
        data class Success(val added: Int, val skipped: Int) : ImportResult()
        data class Failure(val message: String) : ImportResult()
    }

    sealed class ActorsRefreshResult {
        data class Success(val count: Int) : ActorsRefreshResult()
        data class Failure(val message: String) : ActorsRefreshResult()
    }

    sealed class ActorNamesRefreshResult {
        data class Success(val updatedCount: Int) : ActorNamesRefreshResult()
        data class Failure(val message: String) : ActorNamesRefreshResult()
    }

    // === Article import ===
    sealed class ArticleImportState {
        object Idle : ArticleImportState()
        object Parsing : ArticleImportState()
        object Resolving : ArticleImportState()
        object Ready : ArticleImportState()
        object Saving : ArticleImportState()
        data class Error(val message: String) : ArticleImportState()
    }

    sealed class ArticleImportSaveResult {
        data class Success(val added: Int, val skipped: Int) : ArticleImportSaveResult()
        data class Failure(val message: String) : ArticleImportSaveResult()
    }

    private val _articleImportUrl = MutableStateFlow("")
    val articleImportUrl = _articleImportUrl.asStateFlow()

    /** Название категории: после парсинга заполняется заголовком страницы, пользователь может редактировать. */
    private val _articleImportCategoryName = MutableStateFlow("")
    val articleImportCategoryName = _articleImportCategoryName.asStateFlow()

    private val _articleImportMediaType = MutableStateFlow("movie")
    val articleImportMediaType = _articleImportMediaType.asStateFlow()

    private val _articleImportItems = MutableStateFlow<List<com.sh.video.videolibrary.ui.screens.ArticleImportItem>>(emptyList())
    val articleImportItems = _articleImportItems.asStateFlow()

    private val _articleImportState = MutableStateFlow<ArticleImportState>(ArticleImportState.Idle)
    val articleImportState = _articleImportState.asStateFlow()

    private val _articleImportSaveResult = MutableStateFlow<ArticleImportSaveResult?>(null)
    val articleImportSaveResult = _articleImportSaveResult.asStateFlow()

    private val _articleImportDetailItem = MutableStateFlow<com.sh.video.videolibrary.ui.screens.ArticleImportItem?>(null)
    val articleImportDetailItem = _articleImportDetailItem.asStateFlow()

    fun setArticleImportUrl(url: String) {
        _articleImportUrl.value = url
        _articleImportSaveResult.value = null
    }

    fun setArticleImportCategoryName(name: String) {
        _articleImportCategoryName.value = name
    }

    fun setArticleImportMediaType(type: String) {
        _articleImportMediaType.value = type
    }

    fun setArticleImportItemChecked(itemId: String, checked: Boolean) {
        _articleImportItems.value = _articleImportItems.value.map {
            if (it.id == itemId) it.copy(isChecked = checked) else it
        }
    }

    fun selectArticleImportItemForDetail(item: com.sh.video.videolibrary.ui.screens.ArticleImportItem) {
        _articleImportDetailItem.value = item
    }

    fun clearArticleImportDetail() {
        _articleImportDetailItem.value = null
    }

    fun analyzeArticleForImport() {
        viewModelScope.launch {
            _articleImportState.value = ArticleImportState.Parsing
            _articleImportSaveResult.value = null
            try {
                val result = repository.parseArticle(_articleImportUrl.value.trim())
                if (result.isFailure) {
                    _articleImportState.value = ArticleImportState.Error(
                        result.exceptionOrNull()?.message ?: app.getString(R.string.error_generic)
                    )
                    return@launch
                }
                val parseResult = result.getOrThrow()
                val titles = parseResult.movieTitles
                if (titles.isEmpty()) {
                    _articleImportState.value = ArticleImportState.Error(app.getString(R.string.import_article_no_titles))
                    return@launch
                }
                // Заполняем поле категории заголовком страницы
                _articleImportCategoryName.value = parseResult.pageTitle
                _articleImportState.value = ArticleImportState.Resolving
                val mediaType = _articleImportMediaType.value
                val items = mutableListOf<com.sh.video.videolibrary.ui.screens.ArticleImportItem>()
                for ((index, parsedTitle) in titles.withIndex()) {
                    val searchQuery = ArticleParser.stripYearForSearch(parsedTitle)
                    val details = runCatching {
                        when (mediaType) {
                            "tv" -> {
                                val resp = repository.searchTmdbTv(searchQuery)
                                resp.results.firstOrNull()?.let { r ->
                                    repository.getTmdbTvDetails(r.id)
                                }?.let { TmdbMediaDetails.Tv(it) }
                            }
                            else -> {
                                val resp = repository.searchTmdbMovies(searchQuery)
                                resp.results.firstOrNull()?.let { r ->
                                    repository.getTmdbMovieDetails(r.id)
                                }?.let { TmdbMediaDetails.Movie(it) }
                            }
                        }
                    }.getOrNull()
                    items.add(
                        com.sh.video.videolibrary.ui.screens.ArticleImportItem(
                            id = "article_${index}_$parsedTitle",
                            parsedTitle = parsedTitle,
                            tmdbDetails = details,
                            isChecked = details != null,
                            isLoading = false
                        )
                    )
                }
                _articleImportItems.value = items
                _articleImportState.value = ArticleImportState.Ready
            } catch (e: Exception) {
                _articleImportState.value = ArticleImportState.Error(e.message ?: app.getString(R.string.error_generic))
            }
        }
    }

    fun saveArticleImportSelected() {
        viewModelScope.launch {
            val categoryName = _articleImportCategoryName.value.trim()
            if (categoryName.isBlank()) {
                _articleImportSaveResult.value = ArticleImportSaveResult.Failure(app.getString(R.string.import_article_no_category))
                return@launch
            }
            // Создаём категорию при сохранении (или получаем существующую)
            val categoryId = repository.addCategory(categoryName)
            _articleImportState.value = ArticleImportState.Saving
            _articleImportSaveResult.value = null
            try {
                var added = 0
                var skipped = 0
                for (item in _articleImportItems.value.filter { it.isChecked && it.tmdbDetails != null }) {
                    val id = repository.addMediaWithCategory(item.tmdbDetails!!, categoryId)
                    if (id > 0) added++ else skipped++
                }
                _articleImportSaveResult.value = ArticleImportSaveResult.Success(added, skipped)
                _articleImportState.value = ArticleImportState.Ready
            } catch (e: Exception) {
                _articleImportState.value = ArticleImportState.Ready
                _articleImportSaveResult.value = ArticleImportSaveResult.Failure(e.message ?: app.getString(R.string.error_generic))
            }
        }
    }

    fun clearArticleImportSaveResult() {
        _articleImportSaveResult.value = null
    }

    private val _pendingAddToArticleImport = MutableStateFlow(false)
    val pendingAddToArticleImport = _pendingAddToArticleImport.asStateFlow()

    fun setPendingAddToArticleImport(value: Boolean) {
        _pendingAddToArticleImport.value = value
    }

    /** Добавляет фильм/сериал из TMDB в список импорта (ручное добавление). */
    fun addArticleImportItemFromTmdb(details: TmdbMediaDetails) {
        val title = details.title
        val item = com.sh.video.videolibrary.ui.screens.ArticleImportItem(
            id = "manual_${details.id}_${details.mediaType}",
            parsedTitle = title,
            tmdbDetails = details,
            isChecked = true,
            isLoading = false
        )
        if (_articleImportItems.value.none { it.id == item.id }) {
            _articleImportItems.value = _articleImportItems.value + item
        }
        _pendingAddToArticleImport.value = false
    }

    fun resetArticleImport() {
        _articleImportUrl.value = ""
        _articleImportCategoryName.value = ""
        _articleImportItems.value = emptyList()
        _articleImportState.value = ArticleImportState.Idle
        _articleImportSaveResult.value = null
        _articleImportDetailItem.value = null
        _pendingAddToArticleImport.value = false
    }

    // ViewModel needs Context - we hold it weakly
    private var context: Context = context
}

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
