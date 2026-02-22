package com.sh.video.videolibrary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.sh.video.videolibrary.R
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sh.video.videolibrary.data.remote.TmdbMediaDetails
import com.sh.video.videolibrary.ui.MainViewModel
import com.sh.video.videolibrary.ui.components.FullScreenImageDialog
import com.sh.video.videolibrary.ui.components.TMDB_IMAGE_BASE
import com.sh.video.videolibrary.ui.components.TMDB_IMAGE_ORIGINAL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TmdbMoviePreviewScreen(
    movie: TmdbMediaDetails,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onMovieAdded: (movieId: Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val movieAddedSuccess by viewModel.movieAddedSuccess.collectAsState()
    val movieIdJustAdded by viewModel.movieIdJustAdded.collectAsState()
    val tmdbPreviewActors by viewModel.tmdbPreviewActors.collectAsState()
    val showAlreadyInCollection = uiState is MainViewModel.UiState.Error

    LaunchedEffect(movie.id, movie.mediaType) {
        viewModel.loadTmdbPreviewActors(movie.id, movie.mediaType)
    }

    LaunchedEffect(movieAddedSuccess, movieIdJustAdded) {
        if (movieAddedSuccess && movieIdJustAdded != null) {
            val id = movieIdJustAdded!!
            viewModel.clearTmdbPreview()
            viewModel.clearMovieAddedSuccess()
            onMovieAdded(id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(movie.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
            var showFullScreenPoster by remember { mutableStateOf(false) }
            if (showFullScreenPoster && movie.posterPath != null) {
                FullScreenImageDialog(
                    imageUrl = TMDB_IMAGE_ORIGINAL + movie.posterPath,
                    contentDescription = movie.title,
                    onDismiss = { showFullScreenPoster = false }
                )
            }
            Row {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(180.dp)
                        .then(
                            if (movie.posterPath != null) Modifier.clickable { showFullScreenPoster = true }
                            else Modifier
                        )
                ) {
                    AsyncImage(
                        model = if (movie.posterPath != null) TMDB_IMAGE_BASE + movie.posterPath else null,
                        contentDescription = movie.title,
                        modifier = Modifier
                            .width(120.dp)
                            .height(180.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(movie.title, style = MaterialTheme.typography.headlineSmall)
                    movie.originalTitle?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("★ ${movie.voteAverage ?: 0}", style = MaterialTheme.typography.bodyLarge)
                    movie.releaseDate?.take(4)?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            movie.genres?.takeIf { it.isNotEmpty() }?.let { genres ->
                Text(stringResource(R.string.genres_label, genres.joinToString { it.name }), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }
            if (tmdbPreviewActors.isNotEmpty()) {
                Text(stringResource(R.string.cast), style = MaterialTheme.typography.titleSmall)
                Text(tmdbPreviewActors.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
            }
            movie.overview?.takeIf { it.isNotBlank() }?.let { overview ->
                Text(stringResource(R.string.description), style = MaterialTheme.typography.titleSmall)
                Text(overview, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
            }
            if (showAlreadyInCollection) {
                Text(
                    (uiState as? MainViewModel.UiState.Error)?.message ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Button(
                onClick = { viewModel.addMovie(movie) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (movie.mediaType == "tv") stringResource(R.string.add_tv_to_collection) else stringResource(R.string.add_to_collection))
            }
        }
    }
}
