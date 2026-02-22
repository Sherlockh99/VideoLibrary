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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.sh.video.videolibrary.data.repository.CategoryWithMovieCount
import com.sh.video.videolibrary.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onCategoryClick: (CategoryWithMovieCount) -> Unit = {}
) {
    val categoriesWithCounts by viewModel.categoriesWithMovieCounts.collectAsState()
    val categoryRemoveError by viewModel.categoryRemoveError.collectAsState()
    var categoryToDelete by remember { mutableStateOf<CategoryWithMovieCount?>(null) }
    var categoryToEdit by remember { mutableStateOf<CategoryWithMovieCount?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    if (categoryToEdit != null) {
        val item = categoryToEdit!!
        var newName by remember(item) { mutableStateOf(item.category.name) }
        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text(stringResource(R.string.edit_category_title)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.label_category_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.updateCategory(item.category.id, newName.trim())
                            categoryToEdit = null
                        }
                    },
                    enabled = newName.isNotBlank() && newName.trim() != item.category.name
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToEdit = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (categoryToDelete != null) {
        val item = categoryToDelete!!
        val category = item.category
        val canDelete = item.movieCount == 0
        AlertDialog(
            onDismissRequest = {
                categoryToDelete = null
                viewModel.clearCategoryRemoveError()
            },
            title = { Text(stringResource(R.string.delete_category_title)) },
            text = {
                if (canDelete) {
                    Text(stringResource(R.string.delete_category_confirm, category.name))
                } else {
                    Text(stringResource(R.string.delete_category_has_movies, category.name, item.movieCount))
                }
            },
            confirmButton = {
                if (canDelete) {
                    TextButton(onClick = {
                        viewModel.removeCategory(category.id)
                        categoryToDelete = null
                    }) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    categoryToDelete = null
                    viewModel.clearCategoryRemoveError()
                }) {
                    Text(if (canDelete) stringResource(R.string.cancel) else stringResource(R.string.got_it))
                }
            }
        )
    }

    if (showAddCategoryDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text(stringResource(R.string.add_category_title)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.label_category_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.addCategory(newName.trim())
                            showAddCategoryDialog = false
                        }
                    },
                    enabled = newName.isNotBlank()
                ) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddCategoryDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.categories_add))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (categoryRemoveError != null) {
                Text(
                    text = categoryRemoveError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (categoriesWithCounts.isEmpty()) {
                Text(
                    text = stringResource(R.string.categories_empty),
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoriesWithCounts) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategoryClick(item) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.category.name)
                                if (item.movieCount > 0) {
                                    Text(
                                        text = stringResource(R.string.categories_movies_count, item.movieCount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            IconButton(onClick = { categoryToEdit = item }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit_name),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { categoryToDelete = item }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = if (item.movieCount == 0)
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
