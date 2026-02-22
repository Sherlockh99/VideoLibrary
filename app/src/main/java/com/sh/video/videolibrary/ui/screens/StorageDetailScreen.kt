package com.sh.video.videolibrary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sh.video.videolibrary.R
import com.sh.video.videolibrary.data.local.FileOnStorageRow
import com.sh.video.videolibrary.data.local.StorageEntity
import com.sh.video.videolibrary.ui.MainViewModel
import com.sh.video.videolibrary.ui.StorageFileFilter
import com.sh.video.videolibrary.ui.components.TMDB_IMAGE_BASE
import com.sh.video.videolibrary.ui.components.formatFileSize

private fun getStorageFileFilterLabelRes(filter: StorageFileFilter): Int = when (filter) {
    StorageFileFilter.ALL -> R.string.storage_filter_all
    StorageFileFilter.BY_MOVIE_TITLE -> R.string.storage_filter_by_title
    StorageFileFilter.BY_GENRE -> R.string.storage_filter_by_genre
    StorageFileFilter.BY_TMDB_RATING -> R.string.storage_filter_by_tmdb
    StorageFileFilter.BY_PERSONAL_RATING -> R.string.storage_filter_by_personal
    StorageFileFilter.BY_FILE_NAME -> R.string.storage_filter_by_filename
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageDetailScreen(
    storage: StorageEntity,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onFileClick: (Long) -> Unit
) {
    val filteredFiles by viewModel.filteredStorageFiles.collectAsState()
    val storageFileFilter by viewModel.storageFileFilter.collectAsState()
    val storageFileFilterQuery by viewModel.storageFileFilterQuery.collectAsState()
    val storageFileFilterTmdbMinRating by viewModel.storageFileFilterTmdbMinRating.collectAsState()
    val storageFileFilterPersonalRating by viewModel.storageFileFilterPersonalRating.collectAsState()
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<FileOnStorageRow?>(null) }
    var showAddFileDialog by remember { mutableStateOf(false) }
    val hasFilter = storageFileFilter != StorageFileFilter.ALL

    LaunchedEffect(storage.id) {
        viewModel.loadStorageFiles(storage.id)
    }

    if (fileToDelete != null) {
        val row = fileToDelete!!
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text(stringResource(R.string.delete_file_from_storage_title)) },
            text = {
                Text(stringResource(R.string.delete_file_from_storage_text, row.fileName, storage.name, row.movieTitle))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeFileFromStorage(row.fileId, storage.id)
                    fileToDelete = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${storage.name}${if (hasFilter) " *" else ""}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { filterMenuExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.filter))
                        }
                        DropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false }
                        ) {
                            StorageFileFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(getStorageFileFilterLabelRes(filter))) },
                                    onClick = {
                                        viewModel.setStorageFileFilter(filter)
                                        filterMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddFileDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_file))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (storageFileFilter != StorageFileFilter.ALL) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (storageFileFilter) {
                        StorageFileFilter.BY_MOVIE_TITLE -> OutlinedTextField(
                            value = storageFileFilterQuery,
                            onValueChange = { viewModel.setStorageFileFilterQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.placeholder_movie_title)) }
                        )
                        StorageFileFilter.BY_GENRE -> OutlinedTextField(
                            value = storageFileFilterQuery,
                            onValueChange = { viewModel.setStorageFileFilterQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.placeholder_genre)) }
                        )
                        StorageFileFilter.BY_TMDB_RATING -> OutlinedTextField(
                            value = storageFileFilterTmdbMinRating?.toString() ?: "",
                            onValueChange = { viewModel.setStorageFileFilterTmdbMinRating(it.toDoubleOrNull()) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.placeholder_min_rating)) }
                        )
                        StorageFileFilter.BY_PERSONAL_RATING -> Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            (1..5).forEach { r ->
                                val selected = storageFileFilterPersonalRating == r
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        viewModel.setStorageFileFilterPersonalRating(if (selected) null else r)
                                    },
                                    label = { Text("$r") }
                                )
                            }
                        }
                        StorageFileFilter.BY_FILE_NAME -> OutlinedTextField(
                            value = storageFileFilterQuery,
                            onValueChange = { viewModel.setStorageFileFilterQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.label_file_name)) }
                        )
                        else -> {}
                    }
                }
            }
            if (filteredFiles.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        if (hasFilter) stringResource(R.string.storage_no_files_filter) else stringResource(R.string.storage_no_files)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFiles) { row ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFileClick(row.movieId) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = if (row.posterPath != null) TMDB_IMAGE_BASE + row.posterPath else null,
                                    contentDescription = row.movieTitle,
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(90.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = row.movieTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2
                                    )
                                    if (row.releaseDate.isNotBlank()) {
                                        Text(
                                            text = row.releaseDate.take(4),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (row.genres.isNotBlank()) {
                                        Text(
                                            text = row.genres,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "★ ${row.rating}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        row.personalRating?.let { pr ->
                                            Text(
                                                text = "  ★ $pr",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Text(
                                        text = row.fileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatFileSize(row.fileSize),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { fileToDelete = row }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.remove_from_storage),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddFileDialog) {
        AddFileToStorageDialog(
            storage = storage,
            viewModel = viewModel,
            onDismiss = {
                showAddFileDialog = false
                viewModel.clearSearchResults()
                viewModel.clearUiStateError()
            },
            onSuccess = { }
        )
    }
}
