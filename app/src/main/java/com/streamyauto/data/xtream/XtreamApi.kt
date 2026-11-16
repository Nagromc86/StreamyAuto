package com.streamyauto.data.xtream

import retrofit2.http.GET
import retrofit2.http.Query

interface XtreamApi {

    @GET("player_api.php")
    suspend fun authenticate(
        @Query("username") username: String,
        @Query("password") password: String
    ): Map<String, @JvmSuppressWildcards Any?>

    @GET("player_api.php")
    suspend fun getLiveCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): List<XtreamCategory>

    @GET("player_api.php")
    suspend fun getLiveStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String? = null
    ): List<XtreamStream>
}

// Champs nullables : les panneaux Xtream ne sont pas tous cohérents
data class XtreamCategory(
    val category_id: String?,
    val category_name: String?
)

data class XtreamStream(
    val num: Int?,
    val name: String?,
    val stream_type: String?,
    val stream_id: Int,
    val stream_icon: String?,
    val category_id: String?
)
