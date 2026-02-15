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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var newName by remember { mutableStateOf("") }
    var storageToDelete by remember { mutableStateOf<StorageWithFileCount?>(null) }

    if (storageToDelete != null) {
        val item = storageToDelete!!
        val storage = item.storage
        val canDelete = item.fileCount == 0
        AlertDialog(
            onDismissRequest = {
                storageToDelete = null
                viewModel.clearStorageRemoveError()
            },
            title = { Text("Удалить хранилище?") },
            text = {
                if (canDelete) {
                    Text("Хранилище \"${storage.name}\" будет удалено.")
                } else {
                    Text("Нельзя удалить: к хранилищу \"${storage.name}\" привязаны файлы (${item.fileCount}). Сначала удалите привязки файлов в карточках фильмов.")
                }
            },
            confirmButton = {
                if (canDelete) {
                    TextButton(onClick = {
                        viewModel.removeStorage(storage.id)
                        storageToDelete = null
                    }) {
                        Text("Удалить", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    storageToDelete = null
                    viewModel.clearStorageRemoveError()
                }) {
                    Text(if (canDelete) "Отмена" else "Понятно")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Хранилища") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Добавьте хранилища (HDD, SSD, M2…). Файлы фильмов добавляйте в карточке фильма (Моя коллекция → фильм → «Добавить файл»).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Название хранилища") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.addStorage(newName.trim())
                            newName = ""
                        }
                    }
                ) {
                    Text("Добавить")
                }
            }
            if (storageRemoveError != null) {
                Text(
                    text = storageRemoveError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (storagesWithCounts.isEmpty()) {
                Text(
                    text = "Нет хранилищ. Добавьте первое.",
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.padding(top = 16.dp),
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
                                        text = "Файлов: ${item.fileCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            IconButton(onClick = { storageToDelete = item }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Удалить",
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
