package com.streamyauto.data.xtream

import com.streamyauto.data.model.Account
import com.streamyauto.data.model.Channel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class XtreamRepository {

    private fun createApi(server: String): XtreamApi {
        val baseUrl = if (server.endsWith("/")) server else "$server/"
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(XtreamApi::class.java)
    }

    suspend fun getLiveStreams(account: Account): List<Channel> {
        val api = createApi(account.server)
        val streams = api.getLiveStreams(account.username, account.password)
        return streams.map { stream ->
            Channel(
                streamId = stream.stream_id,
                accountId = account.id,
                name = stream.name,
                icon = stream.stream_icon,
                categoryId = stream.category_id,
                streamUrl = buildStreamUrl(account, stream.stream_id)
            )
        }
    }

    private fun buildStreamUrl(account: Account, streamId: Int): String {
        val base = account.server.trimEnd('/')
        return "$base/live/${account.username}/${account.password}/$streamId.m3u8"
    }
}
