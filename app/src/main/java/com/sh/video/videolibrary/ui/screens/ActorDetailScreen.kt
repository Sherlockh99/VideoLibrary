package com.sh.video.videolibrary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sh.video.videolibrary.R
import com.sh.video.videolibrary.data.local.ActorEntity
import com.sh.video.videolibrary.data.local.MovieEntity
import com.sh.video.videolibrary.ui.MainViewModel
import com.sh.video.videolibrary.ui.components.MovieCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActorDetailScreen(
    actor: ActorEntity,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onMovieClick: (MovieEntity) -> Unit,
    onGenreClick: (Long) -> Unit = {},
    onAddFromTmdb: () -> Unit
) {
    val actorMovies by viewModel.actorMovies.collectAsState()

    LaunchedEffect(actor.id) {
        viewModel.loadActorMovies(actor.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(actor.name, maxLines = 2) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.setPendingActorIdForNewMovie(actor.id)
                    onAddFromTmdb()
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_from_tmdb))
            }
        }
    ) { padding ->
        if (actorMovies.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.no_movies_with_actor))
                Text(
                    stringResource(R.string.add_from_tmdb_hint),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            viewModel.setPendingActorIdForNewMovie(actor.id)
                            onAddFromTmdb()
                        }
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text(stringResource(R.string.add_from_tmdb))
                    }
                }
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
                items(actorMovies) { item ->
                    MovieCard(
                        movie = item.movie,
                        onClick = { onMovieClick(item.movie) },
                        modifier = Modifier.fillMaxWidth(),
                        topActorNames = item.topActorNames,
                        genres = item.genres,
                        onGenreClick = onGenreClick
                    )
                }
            }
        }
    }
}
