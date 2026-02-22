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

    @GET("search/tv")
    suspend fun searchTv(
        @Query("query") query: String,
        @Query("language") language: String = "ru-RU",
        @Query("page") page: Int = 1
    ): TmdbTvSearchResponse

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: Long,
        @Query("language") language: String = "ru-RU"
    ): TmdbTvDetails

    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(
        @Path("movie_id") movieId: Long,
        @Query("language") language: String = "ru-RU"
    ): TmdbCreditsResponse

    @GET("tv/{tv_id}/credits")
    suspend fun getTvCredits(
        @Path("tv_id") tvId: Long,
        @Query("language") language: String = "ru-RU"
    ): TmdbCreditsResponse
}

data class TmdbCreditsResponse(
    val cast: List<TmdbCastMember>?
)

data class TmdbCastMember(
    val id: Long,
    val name: String,
    val character: String?,
    @SerializedName("order") val billingOrder: Int = 0
)

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

data class TmdbTvSearchResponse(
    val results: List<TmdbTvResult>
)

data class TmdbTvResult(
    val id: Long,
    val name: String,
    @SerializedName("original_name") val originalName: String?,
    val overview: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("vote_average") val voteAverage: Double?
)

data class TmdbTvDetails(
    val id: Long,
    val name: String,
    @SerializedName("original_name") val originalName: String?,
    val overview: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    val genres: List<TmdbGenre>?
)

data class TmdbGenre(
    val id: Int,
    val name: String
)

/**
 * Единый тип для фильма или сериала — для использования в UI.
 */
sealed class TmdbMediaDetails {
    abstract val id: Long
    abstract val title: String
    abstract val originalTitle: String?
    abstract val overview: String?
    abstract val releaseDate: String?
    abstract val posterPath: String?
    abstract val voteAverage: Double?
    abstract val genres: List<TmdbGenre>?
    abstract val mediaType: String

    data class Movie(val data: TmdbMovieDetails) : TmdbMediaDetails() {
        override val id get() = data.id
        override val title get() = data.title
        override val originalTitle get() = data.originalTitle
        override val overview get() = data.overview
        override val releaseDate get() = data.releaseDate
        override val posterPath get() = data.posterPath
        override val voteAverage get() = data.voteAverage
        override val genres get() = data.genres
        override val mediaType get() = "movie"
    }

    data class Tv(val data: TmdbTvDetails) : TmdbMediaDetails() {
        override val id get() = data.id
        override val title get() = data.name
        override val originalTitle get() = data.originalName
        override val overview get() = data.overview
        override val releaseDate get() = data.firstAirDate
        override val posterPath get() = data.posterPath
        override val voteAverage get() = data.voteAverage
        override val genres get() = data.genres
        override val mediaType get() = "tv"
    }
}
