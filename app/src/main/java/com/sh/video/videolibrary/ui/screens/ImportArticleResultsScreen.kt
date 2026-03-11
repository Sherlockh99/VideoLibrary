package com.sh.video.videolibrary.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportArticleResultsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onAddFromTmdb: () -> Unit
) {
    val categoryName by viewModel.articleImportCategoryName.collectAsState()
    val items by viewModel.articleImportItems.collectAsState()
    val state by viewModel.articleImportState.collectAsState()
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
                title = { Text(stringResource(R.string.import_article_results_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddFromTmdb,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.import_article_add_from_tmdb))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = categoryName,
                onValueChange = { viewModel.setArticleImportCategoryName(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.import_article_category_label)) },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.import_article_category_from_page)) }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.import_article_results_count, items.size),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    ArticleImportItemRow(
                        item = item,
                        onCheckedChange = { viewModel.setArticleImportItemChecked(item.id, it) },
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
                    categoryName.isNotBlank() &&
                    state !is MainViewModel.ArticleImportState.Saving
            ) {
                if (state is MainViewModel.ArticleImportState.Saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.import_article_save_selected, selectedCount))
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
