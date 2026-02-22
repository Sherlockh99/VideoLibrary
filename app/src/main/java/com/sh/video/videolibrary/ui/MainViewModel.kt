package com.sh.video.videolibrary.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sh.video.videolibrary.VideoLibraryApp
import com.sh.video.videolibrary.data.local.FileOnStorageRow
import com.sh.video.videolibrary.data.repository.ActorWithMovieCount
import com.sh.video.videolibrary.data.repository.CategoryWithMovieCount
import com.sh.video.videolibrary.data.repository.FileWithStorages
import com.sh.video.videolibrary.data.repository.StorageWithFileCount
import com.sh.video.videolibrary.data.local.ActorEntity
import com.sh.video.videolibrary.data.local.CategoryEntity
import com.sh.video.videolibrary.data.local.MovieEntity
import com.sh.video.videolibrary.data.local.StorageEntity
import com.sh.video.videolibrary.data.remote.TmdbMediaDetails
import com.sh.video.videolibrary.data.remote.TmdbMovieDetails
import com.sh.video.videolibrary.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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

class MainViewModel(context: Context) : ViewModel() {

    private val app = context.applicationContext as VideoLibraryApp
    private val repository = MovieRepository(context, app.tmdbApi)

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
                _categoryRemoveError.value = "Нельзя удалить: к категории привязаны фильмы"
            }
        }
    }

    fun clearCategoryRemoveError() {
        _categoryRemoveError.value = null
    }

    private val _movieCategories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val movieCategories = _movieCategories.asStateFlow()

    private val _categoryMovies = MutableStateFlow<List<MovieEntity>>(emptyList())
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
                _categoryMovies.value = repository.getMoviesByCategoryId(categoryId)
            }
        }
    }

    fun removeMovieFromCategory(movieId: Long, categoryId: Long) {
        viewModelScope.launch {
            val currentIds = repository.getCategoryIdsByMovieId(movieId)
            repository.setMovieCategories(movieId, currentIds.filter { it != categoryId })
            if (categoryId == _currentCategoryId) {
                _categoryMovies.value = repository.getMoviesByCategoryId(categoryId)
            }
        }
    }

    fun loadCategoryMovies(categoryId: Long) {
        _currentCategoryId = categoryId
        viewModelScope.launch {
            _categoryMovies.value = repository.getMoviesByCategoryId(categoryId)
        }
    }

    private val _actorMovies = MutableStateFlow<List<MovieEntity>>(emptyList())
    val actorMovies = _actorMovies.asStateFlow()

    fun loadActorMovies(actorId: Long) {
        _currentActorId = actorId
        viewModelScope.launch {
            _actorMovies.value = repository.getMoviesByActorId(actorId)
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

    enum class SearchMode { MOVIE, TV }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _exportResult = MutableStateFlow<ExportResult?>(null)
    val exportResult = _exportResult.asStateFlow()

    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult = _importResult.asStateFlow()

    private val _selectedMovie = MutableStateFlow<MovieEntity?>(null)
    val selectedMovie = _selectedMovie.asStateFlow()

    private val _movieFiles = MutableStateFlow<List<FileWithStorages>>(emptyList())
    val movieFiles = _movieFiles.asStateFlow()

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
        }
    }

    fun selectMovieById(movieId: Long, onSelected: (() -> Unit)? = null) {
        viewModelScope.launch {
            val movie = repository.getMovieById(movieId)
            if (movie != null) {
                _selectedMovie.value = movie
                _movieFiles.value = repository.getFilesByMovieId(movieId)
                _movieCategories.value = repository.getCategoriesByMovieId(movieId)
                onSelected?.invoke()
            }
        }
    }

    fun clearSelectedMovie() {
        _selectedMovie.value = null
        _movieFiles.value = emptyList()
        _movieCategories.value = emptyList()
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
                _storageRemoveError.value = "Нельзя удалить: к хранилищу привязаны файлы"
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
                _uiState.value = UiState.Error(e.message ?: "Ошибка поиска")
            }
        }
    }

    fun selectTmdbForPreview(details: TmdbMediaDetails) {
        _selectedTmdbForPreview.value = details
        if (_uiState.value is UiState.Error) _uiState.value = UiState.Idle
    }

    fun clearTmdbPreview() {
        _selectedTmdbForPreview.value = null
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
                    if (details.mediaType == "tv") "Сериал уже в коллекции" else "Фильм уже в коллекции"
                )
            } else {
                _pendingCategoryIdForNewMovie?.let { catId ->
                    repository.setMovieCategories(id, repository.getCategoryIdsByMovieId(id) + catId)
                    _returnToCategoryIdAfterAdd.value = catId
                    _pendingCategoryIdForNewMovie = null
                    if (catId == _currentCategoryId) {
                        _categoryMovies.value = repository.getMoviesByCategoryId(catId)
                    }
                }
                _pendingActorIdForNewMovie?.let { actorId ->
                    if (actorId == _currentActorId) {
                        _actorMovies.value = repository.getMoviesByActorId(actorId)
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
