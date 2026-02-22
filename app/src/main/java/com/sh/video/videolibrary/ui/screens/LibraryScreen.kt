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
import androidx.compose.ui.unit.dp
import com.sh.video.videolibrary.data.local.MovieEntity
import com.sh.video.videolibrary.ui.LibraryFilter
import com.sh.video.videolibrary.ui.MainViewModel
import com.sh.video.videolibrary.ui.components.MovieCard

private val filterLabels = mapOf(
    LibraryFilter.ALL to "Все фильмы",
    LibraryFilter.BY_TITLE to "По названию",
    LibraryFilter.BY_GENRE to "По жанру",
    LibraryFilter.BY_TMDB_RATING to "По рейтингу TMDb",
    LibraryFilter.BY_PERSONAL_RATING to "По моей оценке",
    LibraryFilter.BY_STORAGE to "По хранилищу"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onAddClick: () -> Unit,
    onMovieClick: (MovieEntity) -> Unit,
    onBack: () -> Unit
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
                title = { Text("Моя коллекция${if (hasFilter) " *" else ""}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { filterMenuExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Фильтр коллекции")
                        }
                        DropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false }
                        ) {
                            LibraryFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(filterLabels[filter] ?: filter.name) },
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
                Icon(Icons.Default.Add, contentDescription = "Добавить")
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
                            placeholder = { Text("Название фильма") }
                        )
                        LibraryFilter.BY_GENRE -> OutlinedTextField(
                            value = filterQuery,
                            onValueChange = { viewModel.setFilterQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Жанр") }
                        )
                        LibraryFilter.BY_TMDB_RATING -> OutlinedTextField(
                            value = filterTmdbMinRating?.toString() ?: "",
                            onValueChange = { viewModel.setFilterTmdbMinRating(it.toDoubleOrNull()) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Мин. рейтинг (0-10)") }
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
                            placeholder = { Text("Название хранилища") }
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
                            Text("Коллекция пуста")
                            Text("Нажмите + чтобы добавить фильм", modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(libraryWithActors) { (movie, topActors) ->
                                MovieCard(
                                    movie = movie,
                                    onClick = { onMovieClick(movie) },
                                    topActorNames = topActors
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
