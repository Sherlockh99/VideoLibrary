package com.sh.video.videolibrary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.sh.video.videolibrary.data.remote.TmdbMediaDetails
import com.sh.video.videolibrary.ui.MainViewModel

/** Элемент списка импорта: название из статьи + результат TMDB. */
data class ArticleImportItem(
    val id: String,
    val parsedTitle: String,
    val tmdbDetails: TmdbMediaDetails?,
    val isChecked: Boolean,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportFromArticleScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToResults: () -> Unit
) {
    val url by viewModel.articleImportUrl.collectAsState()
    val mediaType by viewModel.articleImportMediaType.collectAsState()
    val state by viewModel.articleImportState.collectAsState()

    LaunchedEffect(state) {
        if (state is MainViewModel.ArticleImportState.Ready) {
            onNavigateToResults()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_from_article_title)) },
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
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // === Форма перед анализом ===
            OutlinedTextField(
                value = url,
                onValueChange = { viewModel.setArticleImportUrl(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.import_article_url_label)) },
                placeholder = { Text("https://www.ixbt.com/live/movie/...") },
                singleLine = true,
                enabled = state !is MainViewModel.ArticleImportState.Parsing &&
                    state !is MainViewModel.ArticleImportState.Resolving
            )
            Spacer(Modifier.height(8.dp))

            Text(stringResource(R.string.import_article_type_label), style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = mediaType == "movie",
                    onClick = { viewModel.setArticleImportMediaType("movie") },
                    label = { Text(stringResource(R.string.search_movies)) }
                )
                FilterChip(
                    selected = mediaType == "tv",
                    onClick = { viewModel.setArticleImportMediaType("tv") },
                    label = { Text(stringResource(R.string.search_tv)) }
                )
            }
            Spacer(Modifier.height(16.dp))

            when (state) {
                is MainViewModel.ArticleImportState.Parsing,
                is MainViewModel.ArticleImportState.Resolving -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (state is MainViewModel.ArticleImportState.Resolving)
                                stringResource(R.string.import_article_resolving)
                            else stringResource(R.string.import_article_parsing),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is MainViewModel.ArticleImportState.Error -> {
                    Text(
                        (state as MainViewModel.ArticleImportState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> {}
            }

            Button(
                onClick = { viewModel.analyzeArticleForImport() },
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotBlank() &&
                    state !is MainViewModel.ArticleImportState.Parsing &&
                    state !is MainViewModel.ArticleImportState.Resolving
            ) {
                Text(stringResource(R.string.import_article_analyze))
            }
        }
    }
}

