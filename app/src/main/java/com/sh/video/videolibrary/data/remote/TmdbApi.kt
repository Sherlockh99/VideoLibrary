package com.sh.video.videolibrary.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("language") language: String = "ru-RU",
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Long,
        @Query("language") language: String = "ru-RU"
    ): TmdbMovieDetails
}

data class TmdbSearchResponse(
    val results: List<TmdbMovieResult>
)

data class TmdbMovieResult(
    val id: Long,
    val title: String,
    @SerializedName("original_title") val originalTitle: String?,
    val overview: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("vote_average") val voteAverage: Double?
)

data class TmdbMovieDetails(
    val id: Long,
    val title: String,
    @SerializedName("original_title") val originalTitle: String?,
    val overview: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    val genres: List<TmdbGenre>?
)

data class TmdbGenre(
    val id: Int,
    val name: String
)
