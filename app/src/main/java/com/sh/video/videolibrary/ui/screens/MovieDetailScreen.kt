package com.sh.video.videolibrary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sh.video.videolibrary.data.local.CategoryEntity
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MovieDetailScreen(
    movie: MovieEntity,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onActorClick: (actorId: Long) -> Unit = {}
) {
    val openDetailOnFilesTab by viewModel.openDetailOnFilesTab.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedRating by remember(movie.id) { mutableStateOf(movie.personalRating ?: 0) }

    LaunchedEffect(openDetailOnFilesTab) {
        if (openDetailOnFilesTab) {
            selectedTabIndex = 1
            viewModel.clearOpenDetailOnFilesTab()
        }
    }
    val movieFiles by viewModel.movieFiles.collectAsState()
    val storages by viewModel.storages.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val movieCategories by viewModel.movieCategories.collectAsState()
    val movieTopActors by viewModel.movieTopActors.collectAsState()
    var showAddFileDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToUnlink by remember { mutableStateOf<CategoryEntity?>(null) }
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
                title = { Text(movie.title + if (movie.mediaType == "tv") " (сериал)" else "") },
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
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = if (movie.posterPath != null) TMDB_IMAGE_BASE + movie.posterPath else null,
                    contentDescription = movie.title,
                    modifier = Modifier
                        .width(120.dp)
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(16.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.Top)
                ) {
                    if (movie.originalTitle.isNotBlank()) {
                        Text(movie.originalTitle, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("★ ${movie.rating}", style = MaterialTheme.typography.bodyLarge)
                    if (movie.releaseDate.isNotBlank()) {
                        Text(movie.releaseDate.take(4), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (movie.genres.isNotBlank()) {
                        Text("Жанры: ${movie.genres}", style = MaterialTheme.typography.bodyMedium)
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
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Категории") }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTabIndex) {
                0 -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp)
                ) {
                    if (movieTopActors.isNotEmpty()) {
                        Text("В ролях", style = MaterialTheme.typography.titleSmall)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            movieTopActors.forEachIndexed { index, actor ->
                                if (index > 0) {
                                    Text(", ", style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    text = actor.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { onActorClick(actor.id) }
                                )
                            }
                        }
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
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp)
                ) {
                    val canDeleteMovie = movieFiles.isEmpty()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { editingFile = null; showAddFileDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Добавить файл")
                        }
                        FilledTonalButton(
                            onClick = { showDeleteConfirmDialog = true },
                            enabled = canDeleteMovie,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Удалить фильм")
                        }
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
                            text = { Text("${if (movie.mediaType == "tv") "Сериал" else "Фильм"} \"${movie.title}\" будет удалён из коллекции. Это действие нельзя отменить.") },
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
                2 -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp)
                ) {
                    if (categoryToUnlink != null) {
                        val cat = categoryToUnlink!!
                        AlertDialog(
                            onDismissRequest = { categoryToUnlink = null },
                            title = { Text("Отвязать категорию?") },
                            text = { Text("Категория «${cat.name}» будет отвязана от этого фильма.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    val newIds = movieCategories.map { it.id }.filter { it != cat.id }
                                    viewModel.setMovieCategories(movie.id, newIds)
                                    categoryToUnlink = null
                                }) {
                                    Text("Отвязать", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { categoryToUnlink = null }) {
                                    Text("Отмена")
                                }
                            }
                        )
                    }
                    FilledTonalButton(
                        onClick = { showAddCategoryDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить категорию")
                    }
                    movieCategories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                category.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = { categoryToUnlink = category },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Отвязать категорию",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    if (movieCategories.isEmpty()) {
                        Text(
                            "Нет привязанных категорий. Нажмите «Добавить категорию».",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                    if (showAddCategoryDialog) {
                        AddCategoryDialog(
                            categories = categories,
                            linkedCategoryIds = movieCategories.map { it.id }.toSet(),
                            onDismiss = { showAddCategoryDialog = false },
                            onSelectCategory = { category ->
                                val newIds = movieCategories.map { it.id } + category.id
                                viewModel.setMovieCategories(movie.id, newIds)
                                showAddCategoryDialog = false
                            },
                            onAddAndLink = { name ->
                                viewModel.addCategoryAndLinkToMovie(movie.id, name)
                                showAddCategoryDialog = false
                            }
                        )
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

@Composable
private fun AddCategoryDialog(
    categories: List<CategoryEntity>,
    linkedCategoryIds: Set<Long>,
    onDismiss: () -> Unit,
    onSelectCategory: (CategoryEntity) -> Unit,
    onAddAndLink: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var newCategoryName by remember { mutableStateOf("") }
    val availableCategories = remember(categories, linkedCategoryIds, searchQuery) {
        val q = searchQuery.trim().lowercase()
        categories
            .filter { it.id !in linkedCategoryIds }
            .filter { q.isEmpty() || it.name.lowercase().contains(q) }
            .sortedBy { it.name }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить категорию") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Поиск") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Новая категория") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    TextButton(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onAddAndLink(newCategoryName.trim())
                            }
                        },
                        enabled = newCategoryName.isNotBlank()
                    ) {
                        Text("Создать и добавить")
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Выберите категорию:", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                if (availableCategories.isEmpty()) {
                    Text(
                        if (searchQuery.isNotBlank()) "Ничего не найдено" else "Нет доступных категорий",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(availableCategories) { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectCategory(category) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(category.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}
