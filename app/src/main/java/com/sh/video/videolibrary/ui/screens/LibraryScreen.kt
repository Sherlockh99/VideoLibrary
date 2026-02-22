package com.sh.video.videolibrary.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sh.video.videolibrary.R
import com.sh.video.videolibrary.data.local.MovieEntity
import com.sh.video.videolibrary.ui.LibraryFilter
import com.sh.video.videolibrary.ui.MainViewModel
import com.sh.video.videolibrary.ui.components.MovieCard

private fun getLibraryFilterLabelRes(filter: LibraryFilter): Int = when (filter) {
    LibraryFilter.ALL -> R.string.filter_all
    LibraryFilter.BY_TITLE -> R.string.filter_by_title
    LibraryFilter.BY_GENRE -> R.string.filter_by_genre
    LibraryFilter.BY_TMDB_RATING -> R.string.filter_by_tmdb_rating
    LibraryFilter.BY_PERSONAL_RATING -> R.string.filter_by_personal_rating
    LibraryFilter.BY_STORAGE -> R.string.filter_by_storage
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onAddClick: () -> Unit,
    onMovieClick: (MovieEntity) -> Unit,
    onBack: () -> Unit,
    onGenreClick: (Long) -> Unit = {}
) {
    val libraryWithActors by viewModel.libraryWithTopActors.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val libraryFilter by viewModel.libraryFilter.collectAsState()
    val filterQuery by viewModel.filterQuery.collectAsState()
    val filterPersonalRating by viewModel.filterPersonalRating.collectAsState()
    val filterTmdbMinRating by viewModel.filterTmdbMinRating.collectAsState()
    val storages by viewModel.storages.collectAsState()
    var filterMenuExpanded by remember { mutableStateOf(false) }
    val hasFilter = libraryFilter != LibraryFilter.ALL

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_title) + if (hasFilter) " *" else "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { filterMenuExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.library_filter))
                        }
                        DropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false }
                        ) {
                            LibraryFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(getLibraryFilterLabelRes(filter))) },
                                    onClick = {
                                        viewModel.setLibraryFilter(filter)
                                        filterMenuExpanded = false
                                        when (filter) {
                                            LibraryFilter.ALL -> {
                                                viewModel.setFilterQuery("")
                                                viewModel.setFilterPersonalRating(null)
                                                viewModel.setFilterTmdbMinRating(null)
                                            }
                                            LibraryFilter.BY_STORAGE -> if (storages.isNotEmpty()) {
                                                viewModel.setFilterQuery(storages.first().name)
                                            }
                                            else -> {}
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.library_add))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (libraryFilter != LibraryFilter.ALL) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (libraryFilter) {
                        LibraryFilter.BY_TITLE -> OutlinedTextField(
                            value = filterQuery,
                            onValueChange = { viewModel.setFilterQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.placeholder_movie_title)) }
                        )
                        LibraryFilter.BY_GENRE -> OutlinedTextField(
                            value = filterQuery,
                            onValueChange = { viewModel.setFilterQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.placeholder_genre)) }
                        )
                        LibraryFilter.BY_TMDB_RATING -> OutlinedTextField(
                            value = filterTmdbMinRating?.toString() ?: "",
                            onValueChange = { viewModel.setFilterTmdbMinRating(it.toDoubleOrNull()) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.placeholder_min_rating)) }
                        )
                        LibraryFilter.BY_PERSONAL_RATING -> Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            (1..5).forEach { r ->
                                val selected = filterPersonalRating == r
                                androidx.compose.material3.FilterChip(
                                    selected = selected,
                                    onClick = {
                                        viewModel.setFilterPersonalRating(if (selected) null else r)
                                    },
                                    label = { Text("$r") }
                                )
                            }
                        }
                        LibraryFilter.BY_STORAGE -> OutlinedTextField(
                            value = filterQuery,
                            onValueChange = { viewModel.setFilterQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.placeholder_storage_name)) }
                        )
                        else -> {}
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState is MainViewModel.UiState.Loading -> {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                    libraryWithActors.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(stringResource(R.string.library_empty))
                            Text(stringResource(R.string.library_add_hint), modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(libraryWithActors) { item ->
                                MovieCard(
                                    movie = item.movie,
                                    onClick = { onMovieClick(item.movie) },
                                    topActorNames = item.topActorNames,
                                    genres = item.genres,
                                    onGenreClick = onGenreClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
