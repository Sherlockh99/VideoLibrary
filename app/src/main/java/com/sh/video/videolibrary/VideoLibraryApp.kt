package com.sh.video.videolibrary

import android.app.Application
import com.sh.video.videolibrary.data.remote.TmdbApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

class VideoLibraryApp : Application() {

    lateinit var tmdbApi: TmdbApi
        private set

    override fun onCreate() {
        super.onCreate()
        val apiKey = BuildConfig.TMDB_API_KEY
        val apiKeyInterceptor = Interceptor { chain ->
            val url = chain.request().url.newBuilder()
                .addQueryParameter("api_key", apiKey)
                .build()
            chain.proceed(chain.request().newBuilder().url(url).build())
        }
        val client = OkHttpClient.Builder().addInterceptor(apiKeyInterceptor).build()
        tmdbApi = Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create()
    }
}
