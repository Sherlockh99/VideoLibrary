package com.sh.video.videolibrary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sh.video.videolibrary.data.local.CategoryEntity
import com.sh.video.videolibrary.data.local.MovieEntity
import com.sh.video.videolibrary.ui.MainViewModel
import com.sh.video.videolibrary.ui.components.TMDB_IMAGE_BASE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMovieToCategoryDialog(
    category: CategoryEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val library by viewModel.library.collectAsState()
    val categoryMovies by viewModel.categoryMovies.collectAsState()
    val linkedMovieIds = remember(categoryMovies) { categoryMovies.map { it.id }.toSet() }

    val filteredLibrary = remember(library, searchQuery, linkedMovieIds) {
        val q = searchQuery.trim().lowercase()
        val available = library.filter { it.id !in linkedMovieIds }
        if (q.isEmpty()) available else available.filter {
            it.title.lowercase().contains(q) || it.originalTitle.lowercase().contains(q)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Добавить фильм в «${category.name}»",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Из коллекции",
                style = MaterialTheme.typography.labelMedium
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Поиск по названию") }
            )
            if (filteredLibrary.isEmpty()) {
                Text(
                    if (library.all { it.id in linkedMovieIds }) "Все фильмы уже в этой категории"
                    else "Нет фильмов по запросу",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredLibrary) { movie ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addMovieToCategory(movie.id, category.id)
                                    onSuccess()
                                    onDismiss()
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
                                        .padding(horizontal = 12.dp)
                                ) {
                                    Text(
                                        movie.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2
                                    )
                                    if (movie.releaseDate.isNotBlank()) {
                                        Text(
                                            movie.releaseDate.take(4),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "★ ${movie.rating}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Закрыть")
                }
            }
        }
    }
}
