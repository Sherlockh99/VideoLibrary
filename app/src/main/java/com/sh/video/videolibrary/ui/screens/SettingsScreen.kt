package com.sh.video.videolibrary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sh.video.videolibrary.R
import com.sh.video.videolibrary.ui.MainViewModel
import com.sh.video.videolibrary.util.LocaleHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onAboutClick: () -> Unit = {},
    onLanguageChange: () -> Unit = {}
) {
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    val currentLang = remember { LocaleHelper.getStoredLanguage(context) }
    val exportResult by viewModel.exportResult.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val actorsRefreshResult by viewModel.actorsRefreshResult.collectAsState()
    val actorsRefreshInProgress by viewModel.actorsRefreshInProgress.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
                .padding(24.dp)
        ) {
            SettingsItem(
                title = stringResource(R.string.settings_export),
                subtitle = stringResource(R.string.settings_export_subtitle),
                onClick = onExportClick
            )
            Spacer(modifier = Modifier.height(12.dp))
            SettingsItem(
                title = stringResource(R.string.settings_import),
                subtitle = stringResource(R.string.settings_import_subtitle),
                onClick = onImportClick
            )
            Spacer(modifier = Modifier.height(12.dp))
            SettingsItem(
                title = stringResource(R.string.settings_refresh_actors),
                subtitle = stringResource(R.string.settings_refresh_actors_subtitle),
                onClick = { viewModel.refreshActorsForAllMovies() },
                enabled = !actorsRefreshInProgress
            )
            if (actorsRefreshInProgress) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(start = 4.dp))
                    Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            SettingsItem(
                title = stringResource(R.string.settings_language),
                subtitle = stringResource(R.string.settings_language_subtitle),
                onClick = { showLanguageDialog = true }
            )
            Spacer(modifier = Modifier.height(12.dp))
            SettingsItem(
                title = stringResource(R.string.settings_about),
                subtitle = stringResource(R.string.settings_about_subtitle),
                onClick = onAboutClick
            )

            if (showLanguageDialog) {
                AlertDialog(
                    onDismissRequest = { showLanguageDialog = false },
                    title = { Text(stringResource(R.string.settings_language)) },
                    text = {
                        Column {
                            listOf(
                                LocaleHelper.LANG_SYSTEM to R.string.language_system,
                                LocaleHelper.LANG_RU to R.string.language_russian,
                                LocaleHelper.LANG_EN to R.string.language_english,
                            ).forEach { (code, labelRes) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            LocaleHelper.setLanguage(context, code)
                                            showLanguageDialog = false
                                            onLanguageChange()
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(labelRes),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (currentLang == code) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "✓",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLanguageDialog = false }) {
                            Text(stringResource(R.string.close))
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            when (val result = exportResult) {
                is MainViewModel.ExportResult.Success -> {
                    Text(
                        text = stringResource(R.string.exported_count, result.count),
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
                        text = stringResource(R.string.error_prefix, result.message),
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
            when (val result = actorsRefreshResult) {
                is MainViewModel.ActorsRefreshResult.Success -> {
                    Text(
                        text = stringResource(R.string.actors_refreshed_count, result.count),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    LaunchedEffect(result) {
                        delay(5000)
                        viewModel.clearActorsRefreshResult()
                    }
                }
                is MainViewModel.ActorsRefreshResult.Failure -> {
                    Text(
                        text = stringResource(R.string.error_prefix, result.message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    LaunchedEffect(result) {
                        delay(5000)
                        viewModel.clearActorsRefreshResult()
                    }
                }
                null -> {}
            }
            when (val result = importResult) {
                is MainViewModel.ImportResult.Success -> {
                    Text(
                        text = stringResource(R.string.import_result, result.added, result.skipped),
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
                        text = stringResource(R.string.error_prefix, result.message),
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
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
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
