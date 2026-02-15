package com.sh.video.videolibrary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sh.video.videolibrary.data.local.MovieEntity
import com.sh.video.videolibrary.data.local.StorageEntity
import com.sh.video.videolibrary.data.repository.FileWithStorages
import com.sh.video.videolibrary.data.remote.TmdbMovieDetails
import com.sh.video.videolibrary.ui.MainViewModel
import com.sh.video.videolibrary.ui.components.TMDB_IMAGE_BASE

private fun formatSize(size: Long): String {
    return when {
        size >= 1_000_000_000 -> "%.1f ГБ".format(size / 1_000_000_000.0)
        size >= 1_000_000 -> "%.1f МБ".format(size / 1_000_000.0)
        size >= 1_000 -> "%.1f КБ".format(size / 1_000.0)
        else -> "$size Б"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFileToStorageDialog(
    storage: StorageEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var collectionSearchQuery by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var sizeText by remember { mutableStateOf("") }
    var sourceTmdb by remember { mutableStateOf(true) }
    var selectedTmdb by remember { mutableStateOf<TmdbMovieDetails?>(null) }
    var selectedMovie by remember { mutableStateOf<MovieEntity?>(null) }
    var selectedExistingFileIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var createNewFile by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val searchResults by viewModel.searchResults.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val library by viewModel.library.collectAsState()
    val movieFilesForAdd by viewModel.movieFilesForAddDialog.collectAsState()
    val movieAddedSuccess by viewModel.movieAddedSuccess.collectAsState()
    val isSearching = uiState is MainViewModel.UiState.Loading

    val filteredLibrary = remember(library, collectionSearchQuery) {
        val q = collectionSearchQuery.trim().lowercase()
        if (q.isEmpty()) library else library.filter { it.title.lowercase().contains(q) }
    }

    LaunchedEffect(sourceTmdb) {
        if (!sourceTmdb) {
            viewModel.clearSearchResults()
            selectedTmdb = null
        } else {
            selectedMovie = null
        }
        selectedExistingFileIds = emptySet()
        createNewFile = false
    }

    LaunchedEffect(selectedMovie, selectedTmdb) {
        val movieId = selectedMovie?.id
            ?: selectedTmdb?.let { library.find { m -> m.tmdbId == it.id }?.id }
            ?: -1L
        viewModel.loadMovieFilesForAddDialog(movieId)
        selectedExistingFileIds = emptySet()
        createNewFile = false
    }

    LaunchedEffect(movieAddedSuccess) {
        if (movieAddedSuccess) {
            viewModel.clearMovieAddedSuccess()
            val details = selectedTmdb
            if (details != null) {
                val movie = viewModel.getMovieByTmdbId(details.id)
                if (movie != null) {
                    selectedMovie = movie
                    selectedTmdb = null
                }
            }
        }
    }

    val filesNotOnStorage = remember(movieFilesForAdd, storage.name) {
        movieFilesForAdd.filter { fws -> storage.name !in fws.storageNames }
    }

    fun canAdd(): Boolean {
        val hasMovie = (sourceTmdb && (selectedTmdb != null || selectedMovie != null)) ||
                (!sourceTmdb && selectedMovie != null)
        if (!hasMovie) return false
        return selectedExistingFileIds.isNotEmpty() ||
                (filesNotOnStorage.isEmpty() && fileName.isNotBlank()) ||
                (createNewFile && fileName.isNotBlank())
    }

    fun doAdd() {
        addError = null
        scope.launch {
            val result = when {
                selectedExistingFileIds.isNotEmpty() -> viewModel.linkExistingFilesToStorage(
                    selectedExistingFileIds.toList(),
                    storage.id
                )
                (createNewFile || filesNotOnStorage.isEmpty()) && fileName.isNotBlank() -> when {
                    selectedMovie != null -> viewModel.addFileToStorage(
                        storage.id, selectedMovie!!.id, null,
                        fileName.trim(), sizeText.toLongOrNull() ?: 0L
                    )
                    selectedTmdb != null -> viewModel.addFileToStorage(
                        storage.id, null, selectedTmdb,
                        fileName.trim(), sizeText.toLongOrNull() ?: 0L
                    )
                    else -> {
                        addError = "Выберите фильм"
                        return@launch
                    }
                }
                else -> {
                    addError = "Выберите файлы или создайте новый"
                    return@launch
                }
            }
            result.fold(
                onSuccess = { onSuccess(); onDismiss() },
                onFailure = { addError = it.message ?: "Ошибка" }
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Добавить файл в ${storage.name}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RadioButton(
                        selected = sourceTmdb,
                        onClick = { sourceTmdb = true }
                    )
                    Text("Поиск TMDb")
                    RadioButton(
                        selected = !sourceTmdb,
                        onClick = { sourceTmdb = false }
                    )
                    Text("Из коллекции")
                }

                if (sourceTmdb) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("Название фильма") }
                        )
                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank()) viewModel.searchTmdb(searchQuery.trim())
                            },
                            enabled = !isSearching
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Искать")
                            }
                        }
                    }
                    if (searchResults.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.height(180.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(searchResults) { movie ->
                                val inCollection = library.any { it.tmdbId == movie.id }
                                val isSelected = selectedTmdb?.id == movie.id || selectedMovie?.tmdbId == movie.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (inCollection) {
                                                selectedMovie = library.find { it.tmdbId == movie.id }
                                                selectedTmdb = null
                                            } else {
                                                selectedTmdb = movie
                                                selectedMovie = null
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = if (movie.posterPath != null) TMDB_IMAGE_BASE + movie.posterPath else null,
                                            contentDescription = movie.title,
                                            modifier = Modifier
                                                .width(48.dp)
                                                .height(72.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                movie.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 2
                                            )
                                            movie.releaseDate?.take(4)?.let {
                                                Text(it, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        if (inCollection && isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (!inCollection && selectedTmdb?.id == movie.id) {
                                            Button(
                                                modifier = Modifier.padding(4.dp),
                                                onClick = { viewModel.addMovie(movie) }
                                            ) {
                                                Text("Добавить в коллекцию")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = collectionSearchQuery,
                        onValueChange = { collectionSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Поиск по названию") }
                    )
                    if (filteredLibrary.isEmpty()) {
                        Text(
                            if (library.isEmpty()) "Коллекция пуста. Используйте поиск TMDb."
                            else "Нет фильмов по запросу",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(120.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredLibrary) { movie ->
                                val isSelected = selectedMovie?.id == movie.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedMovie = movie },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedMovie = movie }
                                    )
                                    Text(movie.title, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }
                }

                val hasMovie = selectedMovie != null || selectedTmdb != null
                if (hasMovie) {
                    Text(
                        "Файл",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    if (filesNotOnStorage.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            filesNotOnStorage.forEach { fws ->
                                val isChecked = fws.file.id in selectedExistingFileIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedExistingFileIds = if (isChecked) {
                                                selectedExistingFileIds - fws.file.id
                                            } else {
                                                selectedExistingFileIds + fws.file.id
                                            }
                                            if (!isChecked) createNewFile = false
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedExistingFileIds = if (checked) {
                                                selectedExistingFileIds + fws.file.id
                                            } else {
                                                selectedExistingFileIds - fws.file.id
                                            }
                                            if (checked) createNewFile = false
                                        }
                                    )
                                    Column(modifier = Modifier.padding(start = 8.dp)) {
                                        Text(fws.file.name, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "${formatSize(fws.file.size)} • ${fws.storageNames.joinToString(", ").ifEmpty { "—" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = createNewFile,
                                onCheckedChange = { createNewFile = it; if (it) selectedExistingFileIds = emptySet() }
                            )
                            Text("Создать новый файл", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    if (createNewFile || filesNotOnStorage.isEmpty()) {
                        OutlinedTextField(
                            value = fileName,
                            onValueChange = { fileName = it },
                            label = { Text("Имя файла") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sizeText,
                            onValueChange = { sizeText = it.filter { c -> c.isDigit() } },
                            label = { Text("Размер (байты)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
                addError?.let { err ->
                    Text(
                        err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
                TextButton(
                    onClick = { doAdd() },
                    enabled = canAdd()
                ) {
                    Text("Добавить")
                }
            }
        }
    }
}
