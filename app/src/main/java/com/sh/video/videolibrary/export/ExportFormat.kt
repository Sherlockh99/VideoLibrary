package com.sh.video.videolibrary.export

import com.google.gson.annotations.SerializedName

/**
 * Универсальный формат экспорта .vlp — совместим с Python desktop приложением.
 * См. export_format.md
 */
data class ExportFormat(
    val format: String = "videolibrary",
    val version: Int = 1,
    @SerializedName("exported_at")
    val exportedAt: String,
    val source: String = "android",
    val storages: List<StorageExport>? = null,
    val movies: List<MovieExport>
) {
    data class StorageExport(
        val id: Int,
        val name: String
    )

    data class MovieExport(
        @SerializedName("tmdb_id") val tmdbId: Int,
        val title: String,
        @SerializedName("original_title") val originalTitle: String,
        val genres: String,
        val rating: Double,
        val overview: String,
        @SerializedName("release_date") val releaseDate: String,
        @SerializedName("poster_path") val posterPath: String?,
        @SerializedName("personal_rating") val personalRating: Int?,
        @SerializedName("storage_names") val storageNames: List<String>? = null,
        val files: List<FileExport>? = null
    )

    data class FileExport(
        val name: String,
        val size: Long = 0,
        @SerializedName("storage_names") val storageNames: List<String>? = null
    )
}
