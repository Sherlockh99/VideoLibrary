package com.sh.video.videolibrary.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.sh.video.videolibrary.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sh.video.videolibrary.data.local.GenreEntity
import com.sh.video.videolibrary.data.local.MovieEntity
import com.sh.video.videolibrary.util.GenreHelper

const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w500"
const val TMDB_IMAGE_ORIGINAL = "https://image.tmdb.org/t/p/original"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovieCard(
    movie: MovieEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    topActorNames: List<String> = emptyList(),
    genres: List<GenreEntity> = emptyList(),
    onGenreClick: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = if (movie.posterPath != null) TMDB_IMAGE_BASE + movie.posterPath else null,
                contentDescription = movie.title,
                modifier = Modifier
                    .width(60.dp)
                    .height(90.dp),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = movie.title + if (movie.mediaType == "tv") stringResource(R.string.movie_tv_label) else "",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2
                )
                if (movie.releaseDate.isNotBlank()) {
                    Text(
                        text = movie.releaseDate.take(4),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (genres.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        genres.forEachIndexed { index, genre ->
                            if (index > 0) {
                                Text(", ", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = GenreHelper.getLocalizedName(context, genre),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onGenreClick(genre.id) }
                            )
                        }
                    }
                } else if (movie.genres.isNotBlank()) {
                    Text(
                        text = movie.genres,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (topActorNames.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.cast_format, topActorNames.joinToString(", ")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "★ ${movie.rating}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    movie.personalRating?.let { pr ->
                        Text(
                            text = "  ★ $pr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
