package com.sh.video.videolibrary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sh.video.videolibrary.R

@Composable
fun HomeScreen(
    onCollectionClick: () -> Unit,
    onStoragesClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onActorsClick: () -> Unit = {},
    onGenresClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(48.dp))

        HomeLinkCard(
            title = stringResource(R.string.home_collection),
            subtitle = stringResource(R.string.home_collection_subtitle),
            icon = Icons.Default.LocalMovies,
            onClick = onCollectionClick
        )
        Spacer(modifier = Modifier.height(16.dp))
        HomeLinkCard(
            title = stringResource(R.string.home_storage),
            subtitle = stringResource(R.string.home_storage_subtitle),
            icon = Icons.Default.Folder,
            onClick = onStoragesClick
        )
        Spacer(modifier = Modifier.height(16.dp))
        HomeLinkCard(
            title = stringResource(R.string.home_categories),
            subtitle = stringResource(R.string.home_categories_subtitle),
            icon = Icons.Default.Category,
            onClick = onCategoriesClick
        )
        Spacer(modifier = Modifier.height(16.dp))
        HomeLinkCard(
            title = stringResource(R.string.home_actors),
            subtitle = stringResource(R.string.home_actors_subtitle),
            icon = Icons.Default.Person,
            onClick = onActorsClick
        )
        Spacer(modifier = Modifier.height(16.dp))
        HomeLinkCard(
            title = stringResource(R.string.home_genres),
            subtitle = stringResource(R.string.home_genres_subtitle),
            icon = Icons.Default.MovieFilter,
            onClick = onGenresClick
        )
        Spacer(modifier = Modifier.height(16.dp))
        HomeLinkCard(
            title = stringResource(R.string.home_settings),
            subtitle = stringResource(R.string.home_settings_subtitle),
            icon = Icons.Default.Settings,
            onClick = onSettingsClick
        )
    }
}

@Composable
private fun HomeLinkCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
