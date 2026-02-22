package com.sh.video.videolibrary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.sh.video.videolibrary.data.local.CategoryEntity
import com.sh.video.videolibrary.data.local.MovieEntity
import com.sh.video.videolibrary.ui.MainViewModel
import com.sh.video.videolibrary.ui.components.MovieCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    category: CategoryEntity,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onMovieClick: (MovieEntity) -> Unit,
    onAddFromTmdb: () -> Unit
) {
    val categoryMovies by viewModel.categoryMovies.collectAsState()
    var addMenuExpanded by remember { mutableStateOf(false) }
    var showAddFromCollectionDialog by remember { mutableStateOf(false) }
    var movieToUnlink by remember { mutableStateOf<MovieEntity?>(null) }

    LaunchedEffect(category.id) {
        viewModel.loadCategoryMovies(category.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.name, maxLines = 2) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { addMenuExpanded = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить фильм")
            }
        }
    ) { padding ->
        if (addMenuExpanded) {
            ModalBottomSheet(
                onDismissRequest = { addMenuExpanded = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Добавить фильм в категорию",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                addMenuExpanded = false
                                showAddFromCollectionDialog = true
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LibraryAdd, contentDescription = null)
                        Text("Из коллекции", modifier = Modifier.padding(start = 16.dp))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                addMenuExpanded = false
                                onAddFromTmdb()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null)
                        Text("С TMDb", modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
        if (movieToUnlink != null) {
            val movie = movieToUnlink!!
            AlertDialog(
                onDismissRequest = { movieToUnlink = null },
                title = { Text("Отвязать фильм от категории?") },
                text = { Text("Фильм «${movie.title}» будет отвязан от категории «${category.name}».") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.removeMovieFromCategory(movie.id, category.id)
                        movieToUnlink = null
                    }) {
                        Text("Отвязать", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { movieToUnlink = null }) {
                        Text("Отмена")
                    }
                }
            )
        }
        if (showAddFromCollectionDialog) {
            AddMovieToCategoryDialog(
                category = category,
                viewModel = viewModel,
                onDismiss = { showAddFromCollectionDialog = false },
                onSuccess = { }
            )
        }
        if (categoryMovies.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Нет фильмов в этой категории")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categoryMovies) { (movie, topActors) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MovieCard(
                            movie = movie,
                            onClick = { onMovieClick(movie) },
                            modifier = Modifier.weight(1f),
                            topActorNames = topActors
                        )
                        IconButton(
                            onClick = { movieToUnlink = movie },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Отвязать от категории",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
