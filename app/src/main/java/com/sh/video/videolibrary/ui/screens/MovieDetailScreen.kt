package com.sh.video.videolibrary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sh.video.videolibrary.data.local.MovieEntity
import com.sh.video.videolibrary.ui.MainViewModel
import com.sh.video.videolibrary.ui.components.TMDB_IMAGE_BASE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    movie: MovieEntity,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var selectedRating by remember(movie.id) { mutableStateOf(movie.personalRating ?: 0) }

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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row {
                AsyncImage(
                    model = if (movie.posterPath != null) TMDB_IMAGE_BASE + movie.posterPath else null,
                    contentDescription = movie.title,
                    modifier = Modifier
                        .width(120.dp)
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(16.dp))
                Column {
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
            Spacer(Modifier.height(16.dp))
            if (movie.genres.isNotBlank()) {
                Text("Жанры: ${movie.genres}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            if (movie.overview.isNotBlank()) {
                Text(movie.overview, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
            Text("Ваша оценка:", style = MaterialTheme.typography.titleMedium)
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
    }
}
