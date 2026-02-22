package com.sh.video.videolibrary.util

import android.content.Context
import com.sh.video.videolibrary.R
import com.sh.video.videolibrary.data.local.GenreEntity

object GenreHelper {
    /**
     * Возвращает localized название жанра в соответствии с текущим языком приложения.
     * Для жанров с tmdbGenreId используется строка из ресурсов (меняется при смене языка).
     * Для жанров без tmdbGenreId — fallback на name из БД.
     */
    fun getLocalizedName(context: Context, genre: GenreEntity): String {
        val resId = genre.tmdbGenreId?.let { getGenreNameResId(it) }
        return if (resId != null) context.getString(resId) else genre.name
    }
}

private fun getGenreNameResId(tmdbGenreId: Int): Int? = when (tmdbGenreId) {
    28 -> R.string.genre_28
    12 -> R.string.genre_12
    16 -> R.string.genre_16
    35 -> R.string.genre_35
    80 -> R.string.genre_80
    99 -> R.string.genre_99
    18 -> R.string.genre_18
    10751 -> R.string.genre_10751
    14 -> R.string.genre_14
    36 -> R.string.genre_36
    27 -> R.string.genre_27
    10402 -> R.string.genre_10402
    9648 -> R.string.genre_9648
    10749 -> R.string.genre_10749
    878 -> R.string.genre_878
    10770 -> R.string.genre_10770
    53 -> R.string.genre_53
    10752 -> R.string.genre_10752
    37 -> R.string.genre_37
    10759 -> R.string.genre_10759
    10765 -> R.string.genre_10765
    10763 -> R.string.genre_10763
    10764 -> R.string.genre_10764
    10766 -> R.string.genre_10766
    10767 -> R.string.genre_10767
    10768 -> R.string.genre_10768
    else -> null
}
