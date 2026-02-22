package com.sh.video.videolibrary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sh.video.videolibrary.R
import com.sh.video.videolibrary.data.repository.ActorWithMovieCount
import com.sh.video.videolibrary.ui.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActorsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onActorClick: (ActorWithMovieCount) -> Unit
) {
    val actorsWithCounts by viewModel.actorsWithMovieCounts.collectAsState()
    val actorNamesRefreshResult by viewModel.actorNamesRefreshResult.collectAsState()
    val actorNamesRefreshInProgress by viewModel.actorNamesRefreshInProgress.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val filteredActors = remember(actorsWithCounts, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) actorsWithCounts
        else actorsWithCounts.filter { it.actor.name.lowercase().contains(q) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.actors_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshActorNamesToRussian() },
                        enabled = !actorNamesRefreshInProgress && actorsWithCounts.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.actors_refresh_names))
                    }
                }
            )
        }
    ) { padding ->
        when (val result = actorNamesRefreshResult) {
            is MainViewModel.ActorNamesRefreshResult.Success -> {
                Text(
                    text = stringResource(R.string.actors_updated, result.updatedCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
                LaunchedEffect(result) {
                    delay(5000)
                    viewModel.clearActorNamesRefreshResult()
                }
            }
            is MainViewModel.ActorNamesRefreshResult.Failure -> {
                Text(
                    text = stringResource(R.string.error_prefix, result.message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
                LaunchedEffect(result) {
                    delay(5000)
                    viewModel.clearActorNamesRefreshResult()
                }
            }
            null -> {}
        }
        if (actorNamesRefreshInProgress) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(0.dp))
                Text(stringResource(R.string.actors_refreshing), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (actorsWithCounts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.actors_empty),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.actors_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.actors_search_placeholder)) },
                    singleLine = true
                )
                if (filteredActors.isEmpty()) {
                    Text(
                        text = stringResource(R.string.actors_nothing_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(32.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredActors) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onActorClick(item) }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.actor.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(R.string.actors_movies_count, item.movieCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
