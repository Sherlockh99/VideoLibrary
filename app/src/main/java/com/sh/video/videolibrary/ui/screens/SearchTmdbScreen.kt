package com.sh.video.videolibrary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.sh.video.videolibrary.R
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sh.video.videolibrary.data.remote.TmdbMediaDetails
import com.sh.video.videolibrary.ui.MainViewModel
import com.sh.video.videolibrary.ui.components.TMDB_IMAGE_BASE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTmdbScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onMovieSelected: (TmdbMediaDetails) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val searchMode by viewModel.searchMode.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isSearching = uiState is MainViewModel.UiState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Поиск на TMDb") },
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
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = searchMode == MainViewModel.SearchMode.MOVIE,
                    onClick = { viewModel.setSearchMode(MainViewModel.SearchMode.MOVIE) },
                    label = { Text(stringResource(R.string.search_movies)) }
                )
                FilterChip(
                    selected = searchMode == MainViewModel.SearchMode.TV,
                    onClick = { viewModel.setSearchMode(MainViewModel.SearchMode.TV) },
                    label = { Text(stringResource(R.string.search_tv)) }
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text(if (searchMode == MainViewModel.SearchMode.TV) stringResource(R.string.placeholder_tv_title) else stringResource(R.string.placeholder_movie_title))
                },
                singleLine = true,
                enabled = !isSearching,
                trailingIcon = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            if (query.isNotBlank()) viewModel.searchTmdb(query.trim())
                        },
                        enabled = !isSearching
                    ) {
                        if (isSearching) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(stringResource(R.string.search_button))
                        }
                    }
                }
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { item ->
                    TmdbSearchResultCard(
                        item = item,
                        onClick = { onMovieSelected(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TmdbSearchResultCard(
    item: TmdbMediaDetails,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = if (item.posterPath != null) TMDB_IMAGE_BASE + item.posterPath else null,
                contentDescription = item.title,
                modifier = Modifier
                    .padding(4.dp)
                    .width(60.dp)
                    .height(90.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                if (item.mediaType == "tv") {
                    Text(stringResource(R.string.tv_label), style = MaterialTheme.typography.labelSmall)
                }
                item.releaseDate?.take(4)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text("★ ${item.voteAverage ?: 0}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

