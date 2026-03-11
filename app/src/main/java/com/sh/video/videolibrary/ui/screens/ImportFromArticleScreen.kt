package com.sh.video.videolibrary.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sh.video.videolibrary.R
import com.sh.video.videolibrary.data.remote.TmdbMediaDetails
import com.sh.video.videolibrary.ui.MainViewModel
import com.sh.video.videolibrary.ui.components.TMDB_IMAGE_BASE

/** Элемент списка импорта: название из статьи + результат TMDB. */
data class ArticleImportItem(
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
    onMovieAdded: (() -> Unit)? = null
) {
    val url by viewModel.articleImportUrl.collectAsState()
    val categoryId by viewModel.articleImportCategoryId.collectAsState()
    val categoryNameForNew by viewModel.articleImportCategoryNameForNew.collectAsState()
    val mediaType by viewModel.articleImportMediaType.collectAsState()
    val items by viewModel.articleImportItems.collectAsState()
    val state by viewModel.articleImportState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val saveResult by viewModel.articleImportSaveResult.collectAsState()
    val detailItem by viewModel.articleImportDetailItem.collectAsState()

    detailItem?.let { item ->
        ArticleImportDetailDialog(
            item = item,
            onDismiss = { viewModel.clearArticleImportDetail() }
        )
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

            Text(stringResource(R.string.import_article_category_label), style = MaterialTheme.typography.labelMedium)
            if (categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = categoryId == cat.id,
                            onClick = {
                                viewModel.setArticleImportCategoryId(cat.id)
                                viewModel.setArticleImportCategoryNameForNew("")
                            },
                            label = { Text(cat.name) }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            OutlinedTextField(
                value = categoryNameForNew,
                onValueChange = { viewModel.setArticleImportCategoryNameForNew(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.import_article_new_category)) },
                singleLine = true,
                placeholder = { Text(if (categories.isEmpty()) stringResource(R.string.label_category_name) else "") }
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
                    (categoryId != null || categoryNameForNew.isNotBlank()) &&
                    state !is MainViewModel.ArticleImportState.Parsing &&
                    state !is MainViewModel.ArticleImportState.Resolving
            ) {
                Text(stringResource(R.string.import_article_analyze))
            }

            // === Список результатов ===
            if (items.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.import_article_results_count, items.size),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(350.dp)
                ) {
                    items(items, key = { it.parsedTitle }) { item ->
                        ArticleImportItemRow(
                            item = item,
                            onCheckedChange = { viewModel.setArticleImportItemChecked(item.parsedTitle, it) },
                            onClick = { viewModel.selectArticleImportItemForDetail(item) }
                        )
                    }
                }

                val selectedCount = items.count { it.isChecked }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.saveArticleImportSelected() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedCount > 0 &&
                        state !is MainViewModel.ArticleImportState.Saving
                ) {
                    if (state is MainViewModel.ArticleImportState.Saving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.import_article_save_selected, selectedCount))
                }
            }

            saveResult?.let { result ->
                Spacer(Modifier.height(8.dp))
                Text(
                    when (result) {
                        is MainViewModel.ArticleImportSaveResult.Success ->
                            stringResource(R.string.import_result, result.added, result.skipped)
                        is MainViewModel.ArticleImportSaveResult.Failure ->
                            result.message
                    },
                    color = if (result is MainViewModel.ArticleImportSaveResult.Success)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ArticleImportDetailDialog(
    item: ArticleImportItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(item.tmdbDetails?.title ?: item.parsedTitle)
        },
        text = {
            Column {
                item.tmdbDetails?.overview?.takeIf { it.isNotBlank() }?.let { overview ->
                    Text(overview, style = MaterialTheme.typography.bodyMedium)
                } ?: run {
                    Text(stringResource(R.string.import_article_not_found), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArticleImportItemRow(
    item: ArticleImportItem,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = null
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = onCheckedChange,
                enabled = item.tmdbDetails != null && !item.isLoading
            )
            AsyncImage(
                model = item.tmdbDetails?.posterPath?.let { TMDB_IMAGE_BASE + it },
                contentDescription = item.parsedTitle,
                modifier = Modifier
                    .size(60.dp, 90.dp)
                    .padding(4.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.tmdbDetails?.title ?: item.parsedTitle,
                    style = MaterialTheme.typography.titleMedium
                )
                if (item.tmdbDetails != null) {
                    item.tmdbDetails.releaseDate?.take(4)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    if (item.tmdbDetails.mediaType == "tv") {
                        Text(stringResource(R.string.tv_label), style = MaterialTheme.typography.labelSmall)
                    }
                } else if (item.isLoading) {
                    Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(stringResource(R.string.import_article_not_found), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
