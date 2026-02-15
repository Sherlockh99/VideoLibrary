package com.sh.video.videolibrary.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sh.video.videolibrary.VideoLibraryApp
import com.sh.video.videolibrary.data.repository.FileWithStorages
import com.sh.video.videolibrary.data.local.MovieEntity
import com.sh.video.videolibrary.data.local.StorageEntity
import com.sh.video.videolibrary.data.remote.TmdbMovieDetails
import com.sh.video.videolibrary.data.remote.TmdbSearchResponse
import com.sh.video.videolibrary.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import java.io.File

class MainViewModel(context: Context) : ViewModel() {

    private val app = context.applicationContext as VideoLibraryApp
    private val repository = MovieRepository(context, app.tmdbApi)

    val library: StateFlow<List<MovieEntity>> = repository.getAllMovies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storages: StateFlow<List<StorageEntity>> = repository.getAllStorages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchResults = MutableStateFlow<List<TmdbMovieDetails>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

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

    fun selectMovie(movie: MovieEntity) {
        _selectedMovie.value = movie
        viewModelScope.launch {
            _movieFiles.value = repository.getFilesByMovieId(movie.id)
        }
    }

    fun clearSelectedMovie() {
        _selectedMovie.value = null
        _movieFiles.value = emptyList()
    }

    fun addStorage(name: String) {
        viewModelScope.launch {
            repository.addStorage(name)
        }
    }

    fun removeStorage(id: Long) {
        viewModelScope.launch {
            repository.removeStorage(id)
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

    fun searchTmdb(query: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = repository.searchTmdb(query)
                val details = response.results.mapNotNull { result ->
                    runCatching { repository.getTmdbDetails(result.id) }.getOrNull()
                }
                _searchResults.value = details
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Ошибка поиска")
            }
        }
    }

    fun addMovie(details: TmdbMovieDetails) {
        viewModelScope.launch {
            val id = repository.addMovie(details)
            if (id == -1L) _uiState.value = UiState.Error("Фильм уже в коллекции")
            else _uiState.value = UiState.Success
        }
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
