package com.sh.video.videolibrary.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sh.video.videolibrary.R

@Composable
fun formatFileSize(size: Long): String {
    return when {
        size >= 1_000_000_000 -> stringResource(R.string.size_gb, size / 1_000_000_000.0)
        size >= 1_000_000 -> stringResource(R.string.size_mb, size / 1_000_000.0)
        size >= 1_000 -> stringResource(R.string.size_kb, size / 1_000.0)
        else -> stringResource(R.string.size_bytes, size)
    }
}
