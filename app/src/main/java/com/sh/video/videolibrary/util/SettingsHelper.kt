package com.sh.video.videolibrary.util

import android.content.Context

object SettingsHelper {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_TMDB_API_KEY = "tmdb_api_key"

    fun getTmdbApiKey(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TMDB_API_KEY, "") ?: ""
    }

    fun setTmdbApiKey(context: Context, apiKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TMDB_API_KEY, apiKey.trim())
            .apply()
    }
}
