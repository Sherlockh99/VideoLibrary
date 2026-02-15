package com.sh.video.videolibrary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sh.video.videolibrary.ui.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    val exportResult by viewModel.exportResult.collectAsState()
    val importResult by viewModel.importResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
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
                .padding(24.dp)
        ) {
            SettingsItem(
                title = "Экспорт в .vlp",
                subtitle = "Сохранить коллекцию в файл",
                onClick = onExportClick
            )
            Spacer(modifier = Modifier.height(12.dp))
            SettingsItem(
                title = "Импорт из .vlp",
                subtitle = "Загрузить коллекцию из файла",
                onClick = onImportClick
            )

            Spacer(modifier = Modifier.height(32.dp))
            when (val result = exportResult) {
                is MainViewModel.ExportResult.Success -> {
                    Text(
                        text = "Экспортировано фильмов: ${result.count}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LaunchedEffect(result) {
                        delay(5000)
                        viewModel.clearExportResult()
                    }
                }
                is MainViewModel.ExportResult.Failure -> {
                    Text(
                        text = "Ошибка: ${result.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    LaunchedEffect(result) {
                        delay(5000)
                        viewModel.clearExportResult()
                    }
                }
                null -> {}
            }
            when (val result = importResult) {
                is MainViewModel.ImportResult.Success -> {
                    Text(
                        text = "Добавлено: ${result.added}, пропущено: ${result.skipped}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    LaunchedEffect(result) {
                        delay(5000)
                        viewModel.clearImportResult()
                    }
                }
                is MainViewModel.ImportResult.Failure -> {
                    Text(
                        text = "Ошибка: ${result.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    LaunchedEffect(result) {
                        delay(5000)
                        viewModel.clearImportResult()
                    }
                }
                null -> {}
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
