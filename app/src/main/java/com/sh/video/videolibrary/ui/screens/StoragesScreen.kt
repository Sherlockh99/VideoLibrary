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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
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
import com.sh.video.videolibrary.data.repository.StorageWithFileCount
import com.sh.video.videolibrary.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoragesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onStorageClick: (StorageWithFileCount) -> Unit = {}
) {
    val storagesWithCounts by viewModel.storagesWithFileCounts.collectAsState()
    val storageRemoveError by viewModel.storageRemoveError.collectAsState()
    var storageToDelete by remember { mutableStateOf<StorageWithFileCount?>(null) }
    var storageToEdit by remember { mutableStateOf<StorageWithFileCount?>(null) }
    var showAddStorageDialog by remember { mutableStateOf(false) }

    if (storageToEdit != null) {
        val item = storageToEdit!!
        var newName by remember(item) { mutableStateOf(item.storage.name) }
        AlertDialog(
            onDismissRequest = { storageToEdit = null },
            title = { Text(stringResource(R.string.edit_storage_title)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.label_storage_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.updateStorage(item.storage.id, newName.trim())
                            storageToEdit = null
                        }
                    },
                    enabled = newName.isNotBlank() && newName.trim() != item.storage.name
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { storageToEdit = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (storageToDelete != null) {
        val item = storageToDelete!!
        val storage = item.storage
        val canDelete = item.fileCount == 0
        AlertDialog(
            onDismissRequest = {
                storageToDelete = null
                viewModel.clearStorageRemoveError()
            },
            title = { Text(stringResource(R.string.delete_storage_title)) },
            text = {
                if (canDelete) {
                    Text(stringResource(R.string.delete_storage_confirm, storage.name))
                } else {
                    Text(stringResource(R.string.delete_storage_has_files, storage.name, item.fileCount))
                }
            },
            confirmButton = {
                if (canDelete) {
                    TextButton(onClick = {
                        viewModel.removeStorage(storage.id)
                        storageToDelete = null
                    }) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    storageToDelete = null
                    viewModel.clearStorageRemoveError()
                }) {
                    Text(if (canDelete) stringResource(R.string.cancel) else stringResource(R.string.got_it))
                }
            }
        )
    }

    if (showAddStorageDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddStorageDialog = false },
            title = { Text(stringResource(R.string.add_storage_title)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.label_storage_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.addStorage(newName.trim())
                            showAddStorageDialog = false
                        }
                    },
                    enabled = newName.isNotBlank()
                ) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStorageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.storages_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddStorageDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.storages_add))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (storageRemoveError != null) {
                Text(
                    text = storageRemoveError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (storagesWithCounts.isEmpty()) {
                Text(
                    text = stringResource(R.string.storages_empty),
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(storagesWithCounts) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStorageClick(item) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.storage.name)
                                if (item.fileCount > 0) {
                                    Text(
                                        text = stringResource(R.string.storages_files_count, item.fileCount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            IconButton(onClick = { storageToEdit = item }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit_name),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { storageToDelete = item }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = if (item.fileCount == 0)
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
