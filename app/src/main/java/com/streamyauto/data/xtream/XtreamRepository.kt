package com.streamyauto.data.xtream

import com.streamyauto.data.model.Account
import com.streamyauto.data.model.Channel
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class XtreamRepository {

    // Pas de log HTTP : les URL Xtream contiennent le mot de passe
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun createApi(server: String): XtreamApi {
        val baseUrl = normalizeServer(server) + "/"
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(XtreamApi::class.java)
    }

    /** Renvoie null si OK, sinon un message d'erreur lisible. */
    suspend fun checkAccount(account: Account): String? = try {
        val response = createApi(account.server).authenticate(account.username, account.password)
        val userInfo = response["user_info"] as? Map<*, *>
        val auth = userInfo?.get("auth")?.toString()
        when {
            userInfo == null -> "Réponse inattendue du serveur"
            auth == "1" || auth == "1.0" -> {
                val status = userInfo["status"]?.toString()
                if (status != null && status != "Active") "Compte non actif : $status" else null
            }
            else -> "Identifiants refusés par le serveur"
        }
    } catch (e: Exception) {
        "Connexion impossible : ${e.message ?: e.javaClass.simpleName}"
    }

    suspend fun getLiveCategories(account: Account): List<XtreamCategory> =
        createApi(account.server)
            .getLiveCategories(account.username, account.password)
            .filter { it.category_id != null }

    suspend fun getLiveStreams(account: Account, categoryId: String? = null): List<Channel> {
        val streams = createApi(account.server)
            .getLiveStreams(account.username, account.password, categoryId = categoryId)
        return streams.map { stream ->
            Channel(
                streamId = stream.stream_id,
                accountId = account.id,
                name = stream.name?.trim().orEmpty().ifBlank { "Chaîne ${stream.stream_id}" },
                icon = stream.stream_icon?.takeIf { it.isNotBlank() },
                categoryId = stream.category_id,
                streamUrl = buildStreamUrl(account, stream.stream_id)
            )
        }
    }

    fun buildStreamUrl(account: Account, streamId: Int): String {
        val ext = if (account.streamFormat == "ts") "ts" else "m3u8"
        return "${normalizeServer(account.server)}/live/${account.username}/${account.password}/$streamId.$ext"
    }

    companion object {
        fun normalizeServer(server: String): String {
            val s = server.trim().trimEnd('/')
            return if (s.startsWith("http://") || s.startsWith("https://")) s else "http://$s"
        }
    }
}
