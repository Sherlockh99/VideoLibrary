package com.sh.video.videolibrary.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_LANGUAGE = "language"

    const val LANG_SYSTEM = "system"
    const val LANG_RU = "ru"
    const val LANG_EN = "en"

    fun getStoredLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
    }

    fun setLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }

    fun wrapContext(context: Context): Context {
        val lang = getStoredLanguage(context)
        val locale = when (lang) {
            LANG_RU -> Locale("ru")
            LANG_EN -> Locale.ENGLISH
            else -> return context
        }
        return updateContextLocale(context, locale)
    }

    private fun updateContextLocale(context: Context, locale: Locale): Context {
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            config.setLocales(LocaleList(locale))
            return context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            return context
        }
    }
}
