package com.sh.video.videolibrary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sh.video.videolibrary.data.local.MovieEntity
import com.sh.video.videolibrary.data.local.StorageEntity
import com.sh.video.videolibrary.data.repository.FileWithStorages
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
fun MovieDetailScreen(
    movie: MovieEntity,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedRating by remember(movie.id) { mutableStateOf(movie.personalRating ?: 0) }
    val movieFiles by viewModel.movieFiles.collectAsState()
    val storages by viewModel.storages.collectAsState()
    var showAddFileDialog by remember { mutableStateOf(false) }
    var editingFile by remember { mutableStateOf<FileWithStorages?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<FileWithStorages?>(null) }
    val movieDeleted by viewModel.movieDeleted.collectAsState()

    LaunchedEffect(movieDeleted) {
        if (movieDeleted) {
            viewModel.clearMovieDeleted()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(movie.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Row(modifier = Modifier.padding(vertical = 16.dp)) {
                AsyncImage(
                    model = if (movie.posterPath != null) TMDB_IMAGE_BASE + movie.posterPath else null,
                    contentDescription = movie.title,
                    modifier = Modifier
                        .width(120.dp)
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(movie.title, style = MaterialTheme.typography.headlineSmall)
                    if (movie.originalTitle.isNotBlank()) {
                        Text(movie.originalTitle, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("★ ${movie.rating}", style = MaterialTheme.typography.bodyLarge)
                    if (movie.releaseDate.isNotBlank()) {
                        Text(movie.releaseDate.take(4), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("О фильме") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Файлы") }
                )
            }

            when (selectedTabIndex) {
                0 -> Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp)
                ) {
                    if (movie.genres.isNotBlank()) {
                        Text("Жанры", style = MaterialTheme.typography.titleSmall)
                        Text(movie.genres, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                    }
                    if (movie.overview.isNotBlank()) {
                        Text("Описание", style = MaterialTheme.typography.titleSmall)
                        Text(movie.overview, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                    }
                    Text("Ваша оценка", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..5).forEach { stars ->
                            val isSelected = selectedRating >= stars
                            Text(
                                text = "★",
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.clickable {
                                    selectedRating = stars
                                    viewModel.updateRating(movie.id, stars)
                                }
                            )
                        }
                    }
                }
                1 -> Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp)
                ) {
                    FilledTonalButton(onClick = { editingFile = null; showAddFileDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить файл")
                    }
                    val canDeleteMovie = movieFiles.isEmpty()
                    FilledTonalButton(
                        onClick = { showDeleteConfirmDialog = true },
                        enabled = canDeleteMovie,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Удалить фильм")
                    }
                    if (!canDeleteMovie) {
                        Text(
                            "Сначала удалите все файлы фильма",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (fileToDelete != null) {
                        val fwsToDelete = fileToDelete!!
                        AlertDialog(
                            onDismissRequest = { fileToDelete = null },
                            title = { Text("Удалить файл?") },
                            text = { Text("Файл \"${fwsToDelete.file.name}\" будет удалён из коллекции.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.removeFile(fwsToDelete.file.id)
                                    fileToDelete = null
                                }) {
                                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { fileToDelete = null }) {
                                    Text("Отмена")
                                }
                            }
                        )
                    }
                    if (showDeleteConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirmDialog = false },
                            title = { Text("Удалить фильм?") },
                            text = { Text("Фильм \"${movie.title}\" будет удалён из коллекции. Это действие нельзя отменить.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.removeMovie(movie.id)
                                    showDeleteConfirmDialog = false
                                }) {
                                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                                    Text("Отмена")
                                }
                            }
                        )
                    }
                    if (showAddFileDialog) {
                        FileDialog(
                            editingFile = editingFile,
                            storages = storages,
                            storageNamesToIds = storages.associate { it.name to it.id },
                            onDismiss = {
                                showAddFileDialog = false
                                editingFile = null
                            },
                            onAdd = { name, size, storageIds ->
                                if (name.isNotBlank() && storageIds.isNotEmpty()) {
                                    viewModel.addFile(movie.id, name, size, storageIds)
                                    showAddFileDialog = false
                                }
                            },
                            onSave = { fileId, name, size, storageIds ->
                                if (name.isNotBlank() && storageIds.isNotEmpty()) {
                                    viewModel.updateFile(fileId, name, size, storageIds)
                                    showAddFileDialog = false
                                    editingFile = null
                                }
                            }
                        )
                    }
                    movieFiles.forEach { fws ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    editingFile = fws
                                    showAddFileDialog = true
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(fws.file.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${formatSize(fws.file.size)} • ${if (fws.storageNames.isEmpty()) "—" else fws.storageNames.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            IconButton(onClick = { fileToDelete = fws }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileDialog(
    editingFile: FileWithStorages?,
    storages: List<StorageEntity>,
    storageNamesToIds: Map<String, Long>,
    onDismiss: () -> Unit,
    onAdd: (name: String, size: Long, storageIds: List<Long>) -> Unit,
    onSave: (fileId: Long, name: String, size: Long, storageIds: List<Long>) -> Unit
) {
    val isEdit = editingFile != null
    var name by remember(editingFile) { mutableStateOf(editingFile?.file?.name ?: "") }
    var sizeText by remember(editingFile) { mutableStateOf(editingFile?.file?.size?.toString() ?: "") }
    var selectedStorageIds by remember(editingFile) {
        mutableStateOf(
            if (editingFile != null) {
                editingFile.storageNames.mapNotNull { storageNamesToIds[it] }.toSet()
            } else {
                emptySet()
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Редактировать файл" else "Добавить файл") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя файла") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = sizeText,
                    onValueChange = { sizeText = it.filter { c -> c.isDigit() } },
                    label = { Text("Размер (байты)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (storages.isNotEmpty()) {
                    Text("Хранилища:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 16.dp))
                    storages.forEach { storage ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = storage.id in selectedStorageIds,
                                onCheckedChange = { checked ->
                                    selectedStorageIds = if (checked == true) {
                                        selectedStorageIds + storage.id
                                    } else {
                                        selectedStorageIds - storage.id
                                    }
                                }
                            )
                            Text(storage.name)
                        }
                    }
                } else {
                    Text("Нет хранилищ. Добавьте в Хранилище.", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val size = sizeText.toLongOrNull() ?: 0L
                    val ids = selectedStorageIds.toList()
                    if (isEdit && editingFile != null) {
                        onSave(editingFile.file.id, name.trim(), size, ids)
                    } else {
                        onAdd(name.trim(), size, ids)
                    }
                }
            ) {
                Text(if (isEdit) "Сохранить" else "Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
